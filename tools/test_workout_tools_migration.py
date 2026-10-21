"""Execute the two additive v7 -> v8 ALTER statements against the installed schema."""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text()
BLOCK = SOURCE.split('private fun addWorkoutToolsColumns(db: SQLiteDatabase) {', 1)[1].split('\n    }', 1)[0]
MIGRATION = re.findall(r'db\.execSQL\("([^"\n]+)"\)', BLOCK)


def database():
    db = sqlite3.connect(':memory:')
    db.executescript((ROOT / 'app/src/test/resources/fixtures/schema-v7.sql').read_text())
    return db


class WorkoutToolsMigrationTest(unittest.TestCase):
    def test_all_existing_columns_keep_their_values(self):
        db = database()
        db.execute("INSERT INTO plan_days VALUES ('d','D','Den',0)")
        db.execute("INSERT INTO plan_exercises VALUES ('a','d','A',4,'KG',0,'b','Poznámka')")
        db.execute("INSERT INTO plan_exercises VALUES ('b','d','B',1,'KG',1,'a','')")
        db.execute("INSERT INTO sessions VALUES (11,'d',1000,2000,3600)")
        db.execute("INSERT INTO sessions VALUES (12,'d',3000,NULL,NULL)")
        db.execute("INSERT INTO workout_sets VALUES (21,12,'a',1,'45','11',1)")
        db.execute("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
        db.execute("INSERT INTO measures VALUES (31,'2026-10-21',NULL,NULL)")
        db.execute("INSERT INTO body_metric_values VALUES (41,31,'arm',36.5)")
        columns = {t: [r[1] for r in db.execute(f'PRAGMA table_info({t})')]
                   for (t,) in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name!='sqlite_sequence'").fetchall()}
        def snapshot():
            return {t: db.execute(f'SELECT {",".join(cols)} FROM {t} ORDER BY id').fetchall() for t, cols in columns.items()}
        before = snapshot()
        with db:
            for sql in MIGRATION:
                db.execute(sql)
        self.assertEqual(2, len(MIGRATION))
        self.assertEqual(before, snapshot())
        self.assertEqual([(0,), (0,)], db.execute('SELECT elapsed_offset_seconds FROM sessions ORDER BY id').fetchall())
        self.assertEqual('ok', db.execute('PRAGMA integrity_check').fetchone()[0])
        self.assertEqual([], db.execute('PRAGMA foreign_key_check').fetchall())

    def test_empty_schema_upgrades_without_new_records(self):
        db = database()
        with db:
            for sql in MIGRATION:
                db.execute(sql)
        self.assertEqual([], db.execute('SELECT superset_id FROM plan_exercises').fetchall())
        self.assertEqual([], db.execute('SELECT elapsed_offset_seconds FROM sessions').fetchall())

    def test_failed_upgrade_rolls_back_the_first_alter(self):
        db = database()
        with self.assertRaises(sqlite3.OperationalError):
            db.execute('BEGIN')
            try:
                db.execute(MIGRATION[0])
                db.execute(MIGRATION[0])
            except Exception:
                db.rollback()
                raise
        self.assertNotIn('superset_id', [r[1] for r in db.execute('PRAGMA table_info(plan_exercises)')])


if __name__ == '__main__':
    unittest.main()
