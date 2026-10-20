"""Exercise the actual v6 -> v7 SQL with synthetic data and no Android SDK."""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "app/src/main/java/cz/rezim/app/LocalStore.kt").read_text()
SQL = re.findall(r'db\.execSQL\("([^"\n]+)"\)', SOURCE)
BLOCK = SOURCE.split("if(oldVersion < 7) {", 1)[1].split("\n        }", 1)[0]
MIGRATION = re.findall(r'db\.execSQL\("([^"\n]+)"\)', BLOCK)
BODY_TABLES = [s for s in SQL if s.startswith("CREATE TABLE body_")]
OLD_TABLES = ("sessions", "workout_sets", "runs", "hiit", "measures", "plan_days", "plan_exercises")


def old_database():
    db = sqlite3.connect(":memory:")
    db.executescript((ROOT / "app/src/test/resources/fixtures/schema-v6.sql").read_text())
    return db


def migrate(db):
    with db:
        for sql in MIGRATION + BODY_TABLES:
            db.execute(sql)


class CustomMeasurementMigrationTest(unittest.TestCase):
    def test_existing_rows_and_ids_survive_and_new_values_are_independent(self):
        db = old_database()
        db.execute("INSERT INTO measures VALUES (71,'2026-10-19',83.1,90.0)")
        db.execute("INSERT INTO measures VALUES (75,'2026-10-20',83.0,NULL)")
        db.execute("INSERT INTO sessions VALUES (41,'mon',1000,2000,3600)")
        db.execute("INSERT INTO workout_sets VALUES (101,41,'row',1,'42.5','12',1)")
        db.commit()
        before = {t: db.execute(f'SELECT * FROM "{t}" ORDER BY id').fetchall() for t in OLD_TABLES}
        migrate(db)
        self.assertEqual(before, {t: db.execute(f'SELECT * FROM "{t}" ORDER BY id').fetchall() for t in OLD_TABLES})
        db.execute("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
        cursor = db.execute("INSERT INTO measures (date) VALUES ('2026-10-21')")
        self.assertGreater(cursor.lastrowid, 75)
        db.execute("INSERT INTO body_metric_values (measurement_id,metric_id,cm) VALUES (?,'arm',36.5)", (cursor.lastrowid,))
        self.assertEqual((None, None), db.execute("SELECT kg,waist FROM measures WHERE id=?", (cursor.lastrowid,)).fetchone())
        self.assertEqual("ok", db.execute("PRAGMA integrity_check").fetchone()[0])
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())

    def test_empty_database_can_upgrade_and_store_just_a_circumference(self):
        db = old_database()
        migrate(db)
        db.execute("INSERT INTO measures (date,waist) VALUES ('2026-10-21',90.0)")
        self.assertEqual((None,90.0), db.execute("SELECT kg,waist FROM measures").fetchone())

    def test_duplicate_custom_values_and_invalid_ranges_are_rejected(self):
        db = old_database()
        migrate(db)
        db.execute("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
        db.execute("INSERT INTO measures (id,date) VALUES (1,'2026-10-21')")
        db.execute("INSERT INTO body_metric_values (measurement_id,metric_id,cm) VALUES (1,'arm',36.5)")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("INSERT INTO body_metric_values (measurement_id,metric_id,cm) VALUES (1,'arm',36.5)")
        db.execute("INSERT INTO measures (id,date) VALUES (2,'2026-10-22')")
        for value in (0, -1, 301):
            with self.subTest(value=value), self.assertRaises(sqlite3.IntegrityError):
                db.execute("INSERT INTO body_metric_values (measurement_id,metric_id,cm) VALUES (2,'arm',?)", (value,))
        self.assertEqual([(36.5,)], db.execute("SELECT cm FROM body_metric_values").fetchall())


if __name__ == "__main__":
    unittest.main()
