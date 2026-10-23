"""Real resource parity and v9 -> v10 data preservation checks."""
from pathlib import Path
import re
import sqlite3
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]

class LiftAntReadinessTest(unittest.TestCase):
    def test_all_six_catalogs_match_and_keep_every_format_argument(self):
        catalogs=[]
        for suffix in ('','-cs','-de','-it','-fr','-ru'):
            root=ET.parse(ROOT/f'app/src/main/res/values{suffix}/strings.xml').getroot()
            rows={s.attrib['name']: ''.join(s.itertext()) for s in root.findall('string')}
            self.assertEqual(len(root.findall('string')),len(rows))
            if suffix=='': self.assertEqual('LiftAnt',rows.pop('app_name').strip('\"'))
            else: self.assertNotIn('app_name',rows)
            catalogs.append(rows)
        self.assertGreater(len(catalogs[0]),440)
        for rows in catalogs[1:]:
            self.assertEqual(set(catalogs[0]),set(rows))
            for key,value in rows.items():
                self.assertEqual(sorted(re.findall(r'%\d+\$s',catalogs[0][key])), sorted(re.findall(r'%\d+\$s',value)), key)
    def test_migration_keeps_all_existing_columns_and_all_rows(self):
        db=sqlite3.connect(':memory:');db.executescript((ROOT/'app/src/test/resources/fixtures/schema-v9.sql').read_text())
        db.execute("INSERT INTO plan_days VALUES ('d','D','Day',0)")
        db.execute("INSERT INTO plan_exercises VALUES ('x','d','Exercise',4,'KG',0,NULL,'Note',NULL,1)")
        db.execute("INSERT INTO sessions VALUES (11,'d',1000,61000,60,0)")
        db.execute("INSERT INTO workout_sets VALUES (21,11,'x',1,'20,5','10',1)")
        old=db.execute('SELECT * FROM plan_exercises').fetchall();sets=db.execute('SELECT * FROM workout_sets').fetchall()
        source=(ROOT/'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text()
        block=source.split('private fun addLoadMultiplierColumn(db: SQLiteDatabase) {',1)[1].split('\n    }',1)[0]
        sql=re.findall(r'db.execSQL\("([^"\n]+)"\)',block);self.assertEqual(1,len(sql));db.execute(sql[0])
        cols='id,day_id,name,sets,unit,position,partner_id,note,superset_id,superset_start_round'
        self.assertEqual(old,db.execute('SELECT '+cols+' FROM plan_exercises').fetchall())
        self.assertEqual(sets,db.execute('SELECT * FROM workout_sets').fetchall())
        for value in (1,2):db.execute('UPDATE plan_exercises SET load_multiplier=?',(value,))
        for value in (0,3,None):
            with self.assertRaises(sqlite3.IntegrityError):db.execute('UPDATE plan_exercises SET load_multiplier=?',(value,))
        self.assertEqual('ok',db.execute('PRAGMA integrity_check').fetchone()[0])
    def test_new_install_contains_no_seeded_personal_runs(self):
        source=(ROOT/'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text()
        self.assertNotIn('knownRuns',source)
        self.assertNotIn('Dřívější záznam z konverzace',source)
    def test_android_declares_all_six_locales_and_original_app_identity(self):
        config=ET.parse(ROOT/'app/src/main/res/xml/locales_config.xml')
        self.assertEqual({'cs','en','de','it','fr','ru'},{x.attrib['{http://schemas.android.com/apk/res/android}name'] for x in config.getroot()})
        self.assertIn('applicationId = "cz.rezim.app"',(ROOT/'app/build.gradle.kts').read_text())
        self.assertIn('targetSdk = 36',(ROOT/'app/build.gradle.kts').read_text())
if __name__=='__main__':unittest.main()
