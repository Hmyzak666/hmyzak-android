from pathlib import Path
import re,sqlite3,unittest,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
class ProfileGpsContracts(unittest.TestCase):
    def test_all_new_resources_in_all_ten_languages(self):
        base=None
        for suffix in ('','-cs','-de','-it','-fr','-ru','-es','-pt-rBR','-pl','-tr'):
            strings={s.attrib['name']:''.join(s.itertext()) for s in ET.parse(ROOT/f'app/src/main/res/values{suffix}/profile_gps.xml').getroot()}
            self.assertGreaterEqual(len(strings),40)
            if base is None:base=strings
            self.assertEqual(base.keys(),strings.keys())
            for key in base:self.assertEqual(re.findall(r'%\d+\$[sd]',base[key]),re.findall(r'%\d+\$[sd]',strings[key]))
    def test_location_has_no_background_permission_and_map_has_internet(self):
        manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
        permissions={e.attrib[ns+'name'] for e in manifest.findall('uses-permission')}
        self.assertIn('android.permission.FOREGROUND_SERVICE_LOCATION',permissions)
        self.assertNotIn('android.permission.ACCESS_BACKGROUND_LOCATION',permissions)
        self.assertIn('android.permission.INTERNET',permissions) # Explicitly opened online basemap only
        service=next(s for s in manifest.find('application').findall('service') if s.attrib[ns+'name']=='.GpsService')
        self.assertEqual('location',service.attrib[ns+'foregroundServiceType']);self.assertEqual('false',service.attrib[ns+'exported'])
    def test_v12_additive_upgrade_preserves_every_old_column(self):
        db=sqlite3.connect(':memory:');db.execute('CREATE TABLE runs(id INTEGER PRIMARY KEY, date TEXT, km REAL, seconds INTEGER)')
        db.execute("INSERT INTO runs VALUES(7,'2026-10-26',5.01,1801)");before=db.execute('SELECT * FROM runs').fetchall()
        source=(ROOT/'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text().split('private fun addGpsEnergySchema(db:SQLiteDatabase) {')[1].split('\n    }')[0]
        for sql in re.findall(r'db.execSQL\("([^"\n]+)"\)',source):db.execute(sql)
        self.assertEqual(before,db.execute('SELECT * FROM runs').fetchall());self.assertEqual('ok',db.execute('PRAGMA integrity_check').fetchone()[0])
if __name__=='__main__':unittest.main()
