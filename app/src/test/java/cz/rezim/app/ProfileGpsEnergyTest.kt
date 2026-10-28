package cz.rezim.app
import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ProfileGpsEnergyTest {
    private lateinit var app:Application
    @Before fun clean() {app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db");app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()}
    @Test fun profileIsOptionalAndAgeIsNotFalselyExact() {
        assertNull(PersonalProfile().ageRange())
        assertEquals(32..33,PersonalProfile(1993).ageRange(LocalDate.of(2026,10,28)))
        val p=PersonalProfile(1993,180.5,"MALE");assertEquals(p,PersonalProfile.decode(p.encode()))
        for(raw in listOf("{\"birthYear\":1993.5}","{\"birthYear\":\"1993\"}","{\"heightCm\":-5}","{\"sex\":\"invalid\"}")) assertTrue(runCatching {PersonalProfile.decode(raw)}.isFailure)
        val vm=RezimState(app);vm.saveProfile(p);val raw=vm.exportBackup();vm.saveProfile(PersonalProfile());vm.importBackup(raw);assertEquals(p,vm.profile)
    }
    @Test fun kcalDistinguishesTotalAndNetAndNeverInventsWeight() {
        assertNull(EnergyCalculator.estimate("RUNNING",1800,null,5.0))
        assertNull(EnergyCalculator.estimate("OTHER",1800,80.0))
        assertNull(EnergyCalculator.estimate("HIIT",0,80.0))
        val e=EnergyCalculator.estimate("ELLIPTICAL",1800,80.0,intensity="MODERATE")!!
        assertEquals(200.0,e.totalLow,0.0001);assertEquals(160.0,e.activeLow,0.0001)
        val unknown=EnergyCalculator.estimate("ELLIPTICAL",1800,80.0)!!
        assertEquals(200.0,unknown.totalLow,0.0);assertEquals(360.0,unknown.totalHigh,0.0)
        val running=EnergyCalculator.estimate("RUNNING",3600,80.0,9.656064)!!
        assertEquals("12050",running.codes);assertEquals(744.0,running.totalLow,0.0001)
        val bike=EnergyCalculator.estimate("STATIONARY_BIKE",3600,80.0)!!;assertEquals(544.0,bike.totalLow,0.0)
    }
    private fun running()=GpsTrack("test","RUNNING","2026-10-28",phase="RUNNING")
    private fun point(lat:Double=49.0,lon:Double=16.0,ms:Long=1000,accuracy:Double=5.0,segment:Int=0)=TrackPoint(lat,lon,accuracy,ms,segment)
    @Test fun gpsRejectsInaccuracyStalenessJumpsAndStationaryJitter() {
        val t=GpsFilter.accept(running(),point(),1000)
        assertEquals(1,t.points.size)
        for(p in listOf(point(lon=17.0,ms=4000),point(ms=2000,accuracy=90.0),point(lon=16.000001,ms=4000),point(ms=0))) assertEquals(t,GpsFilter.accept(t,p,4000))
        assertEquals(t,GpsFilter.accept(t,point(ms=4000),30000))
        val next=GpsFilter.accept(t,point(lat=49.0001,ms=4000),4000)
        assertTrue(next.meters in 10.0..12.0)
        assertEquals(t.copy(phase="PAUSED"),GpsFilter.accept(t.copy(phase="PAUSED"),point(lat=49.0001,ms=4000),4000))
    }
    @Test fun signalGapsAndPauseSegmentsNeverAddBridgingDistance() {
        val t=GpsFilter.accept(running(),point(),1000)
        val gap=GpsFilter.accept(t,point(lat=50.0,ms=40000),40000)
        assertEquals(0.0,gap.meters,0.0);assertEquals(1,gap.segment)
        val resumed=GpsFilter.accept(t.copy(segment=1),point(lat=50.0,ms=5000,segment=1),5000)
        assertEquals(0.0,resumed.meters,0.0)
    }
    @Test fun checkpointFinishAndBackupAreIdempotentAndPreserveManualHistory() {
        LocalStore(app).use {s->
            s.addRun("2026-10-26",5.0,1800,null,null,"Original")
            val original=s.runs().single()
            val t=GpsFilter.accept(running(),point(),1000).copy(elapsedMs=90000)
            s.saveGps(t);assertEquals(t,s.gpsTracks().single())
            val activeBackup=s.exportJson();s.importJson(activeBackup)
            assertEquals("PAUSED",s.gpsTracks().single().phase)
            val id=s.finishGps(t.id);assertEquals(id,s.finishGps(t.id));assertEquals(2,s.runs().size)
            assertEquals(original,s.runs().first {it.id==original.id})
            s.setEnergyIntensity("cardio:$id","MODERATE")
            val backup=s.exportJson();s.importJson(backup)
            assertEquals(backup,s.exportJson());assertEquals(id,s.gpsTracks().single().runId)
        }
    }
    @Test fun corruptGpsImportRollsBackAllTables() {
        LocalStore(app).use {s->
            s.addRun("2026-10-26",5.0,1800,null,null,"Original")
            s.saveGps(running().copy(elapsedMs=90000));s.finishGps("test")
            val before=s.exportJson();val j=JSONObject(before)
            j.getJSONArray("gps_tracks").getJSONObject(0).put("run_id",9999)
            assertTrue(runCatching {s.importJson(j.toString())}.isFailure)
            assertEquals(before,s.exportJson())
        }
    }
    @Test fun installedSchemaTwelveUpgradePreservesOriginalData() {
        LocalStore(app).use {s->
            s.addRun("2026-10-26",5.01,1801,140,160,"Original")
            s.writableDatabase.execSQL("DROP TABLE gps_tracks")
            s.writableDatabase.execSQL("DROP TABLE energy_options")
            s.writableDatabase.version=12
        }
        LocalStore(app).use {s->
            assertEquals(13,s.readableDatabase.version)
            assertEquals(5.01,s.runs().single().km,0.0);assertEquals(1801,s.runs().single().seconds)
            assertEquals("Original",s.runs().single().note);assertTrue(s.gpsTracks().isEmpty())
        }
    }
    @Test fun schemaTwelveBackupImportsWithoutNewTables() {
        LocalStore(app).use {s->
            s.addRun("2026-10-26",5.0,1800,null,null,"Original")
            val j=JSONObject(s.exportJson()).put("schemaVersion",12);j.remove("gps_tracks");j.remove("energy_options")
            s.importJson(j.toString());assertEquals("Original",s.runs().single().note);assertTrue(s.gpsTracks().isEmpty())
        }
    }
}
