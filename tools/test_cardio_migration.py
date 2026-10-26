"""Execute the actual additive cardio migration and check all ten new catalogs."""
from pathlib import Path
import re
import sqlite3
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
class CardioMigrationTest(unittest.TestCase):
    def test_running_and_hiit_records_survive_actual_sql_migration(self):
        db=sqlite3.connect(':memory:')
        db.executescript((ROOT/'app/src/test/resources/fixtures/schema-v10.sql').read_text())
        db.execute("ALTER TABLE hiit ADD COLUMN activity TEXT NOT NULL DEFAULT 'OTHER'")
        db.execute("INSERT INTO runs VALUES (7,'2026-10-20',10.03,3619,146,162,'Original')")
        db.execute("INSERT INTO hiit VALUES (8,'2026-10-21',15,5,60,120,130,150,'Original',NULL,NULL,NULL,NULL,NULL,'ELLIPTICAL')")
        source=(ROOT/'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text()
        body=source.split('private fun addCardioSchema',1)[1].split('private fun addPublicReleaseSchema',1)[0]
        statements=re.findall(r'db.execSQL\("([^"]+)"\)',body)
        self.assertEqual(5,len(statements))
        for sql in statements: db.execute(sql)
        self.assertEqual((7,'2026-10-20',10.03,3619,146,162,'Original','RUNNING','',None,None),db.execute('SELECT * FROM runs').fetchone())
        self.assertEqual((15,5,60,120,'ELLIPTICAL',None),db.execute('SELECT minutes,rounds,work_seconds,rest_seconds,activity,total_seconds FROM hiit').fetchone())
        db.execute("INSERT INTO runs (date,km,seconds,activity,resistance) VALUES ('2026-10-26',0,90,'ELLIPTICAL',8)")
        self.assertEqual((0,90,8),db.execute("SELECT km,seconds,resistance FROM runs WHERE activity='ELLIPTICAL'").fetchone())
    def test_ten_catalogs_have_exactly_the_same_translated_keys(self):
        expected=None
        for suffix in ('','-cs','-de','-it','-fr','-ru','-es','-pt-rBR','-pl','-tr'):
            catalog=ET.parse(ROOT/f'app/src/main/res/values{suffix}/cardio.xml').getroot()
            values={s.attrib['name']:s.text for s in catalog}
            self.assertTrue(all(v and v.strip() for v in values.values()))
            if expected is None: expected=set(values)
            self.assertEqual(expected,set(values))
            self.assertEqual(len(catalog),len(values))
