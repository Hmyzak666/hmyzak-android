"""Check the actual v8 -> v9 SQL against the preserved installed schema."""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'app/src/main/java/cz/rezim/app/LocalStore.kt').read_text()
BLOCK = SOURCE.split('private fun addSupersetRoundColumn(db: SQLiteDatabase) {', 1)[1].split('\n    }', 1)[0]
SQL = re.findall(r'db\.execSQL\("([^"\n]+)"\)', BLOCK)


class SupersetRoundMigrationTest(unittest.TestCase):
    def database(self):
        db = sqlite3.connect(':memory:')
        db.execute('PRAGMA foreign_keys=ON')
        db.executescript((ROOT / 'app/src/test/resources/fixtures/schema-v8.sql').read_text())
        return db

    def test_actual_upgrade_preserves_all_existing_fields_and_links(self):
        db = self.database()
        db.execute("INSERT INTO plan_days VALUES ('d','D','Day',0)")
        db.execute("INSERT INTO plan_exercises VALUES ('bench','d','Bench',4,'KG',0,'push','Note','g')")
        db.execute("INSERT INTO plan_exercises VALUES ('push','d','Push-ups',1,'REPS',1,'bench','','g')")
        db.execute("INSERT INTO sessions VALUES (11,'d',1000,NULL,NULL,120)")
        db.execute("INSERT INTO workout_sets VALUES (21,11,'bench',1,'45','12',1)")
        db.execute("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
        db.execute("INSERT INTO measures VALUES (31,'2026-10-23',NULL,NULL)")
        db.execute("INSERT INTO body_metric_values VALUES (41,31,'arm',36.5)")
        columns = {t: [r[1] for r in db.execute(f'PRAGMA table_info({t})')]
                   for (t,) in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name!='sqlite_sequence'").fetchall()}
        def snapshot():
            return {t: db.execute(f'SELECT {",".join(cols)} FROM {t} ORDER BY id').fetchall() for t, cols in columns.items()}
        before = snapshot()
        self.assertEqual(1, len(SQL))
        with db:
            for sql in SQL:
                db.execute(sql)
        self.assertEqual(before, snapshot())
        self.assertEqual([(1,), (1,)], db.execute('SELECT superset_start_round FROM plan_exercises ORDER BY id').fetchall())
        self.assertEqual('ok', db.execute('PRAGMA integrity_check').fetchone()[0])
        self.assertEqual([], db.execute('PRAGMA foreign_key_check').fetchall())

    def test_first_last_and_specific_rounds_persist_and_invalid_values_are_rejected(self):
        db = self.database()
        for sql in SQL:
            db.execute(sql)
        db.execute("INSERT INTO plan_exercises (id,day_id,name,sets,unit,position) VALUES ('a','d','A',1,'KG',0)")
        for value in (0, 1, 3, 15):
            db.execute("UPDATE plan_exercises SET superset_start_round=? WHERE id='a'", (value,))
            self.assertEqual(value, db.execute("SELECT superset_start_round FROM plan_exercises WHERE id='a'").fetchone()[0])
        for value in (-1, 16, None):
            with self.subTest(value=value), self.assertRaises(sqlite3.IntegrityError):
                db.execute("UPDATE plan_exercises SET superset_start_round=? WHERE id='a'", (value,))
        self.assertEqual(15, db.execute("SELECT superset_start_round FROM plan_exercises WHERE id='a'").fetchone()[0])


if __name__ == '__main__':
    unittest.main()
