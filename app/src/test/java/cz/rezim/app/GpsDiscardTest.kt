package cz.rezim.app
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class GpsDiscardTest {
    @Test fun discardOnlyTargetUnfinishedRecordAndKeepFinishedHistory() {
        val app=RuntimeEnvironment.getApplication()
        app.deleteDatabase("rezim_local_v1.db")
        LocalStore(app).use {s->
            s.saveGps(GpsTrack("empty","RUNNING","2026-10-29"))
            s.saveGps(GpsTrack("other","WALKING","2026-10-29",elapsedMs=2000))
            s.saveGps(GpsTrack("finished","RUNNING","2026-10-29",elapsedMs=60000,meters=100.0))
            val run=s.finishGps("finished")
            assertTrue(s.discardGps("empty"));assertFalse(s.discardGps("empty"))
            assertEquals(setOf("other","finished"),s.gpsTracks().map {it.id}.toSet())
            assertEquals(run,s.gpsTracks().first {it.id=="finished"}.runId)
            assertThrows(IllegalArgumentException::class.java) {s.discardGps("finished")}
            assertEquals(1,s.readableDatabase.rawQuery("SELECT id FROM runs WHERE id=?",arrayOf(run.toString())).use {it.count})
        }
    }
    @Test fun explicitJsonNullRunIdFromOlderBackupRemainsReadable() {
        val t=GpsTrack("empty","RUNNING","2026-10-29")
        val raw=org.json.JSONObject(t.encode()).put("runId",org.json.JSONObject.NULL).toString()
        assertEquals(t,GpsTrack.decode(raw))
    }
}
