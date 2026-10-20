"""Run with python3 docs/overit_migraci_mereni.py from the project root.

Executes the actual migration SQL from LocalStore.kt using SQLite. Fixtures are
synthetic. Android-specific backup/UI and physical haptics still need a phone.
"""
from pathlib import Path
import re
import sqlite3
import unittest

SOURCE = (Path(__file__).resolve().parents[1] /
          "app/src/main/java/cz/rezim/app/LocalStore.kt").read_text()
V6_BLOCK = SOURCE.split("if(oldVersion < 6) {", 1)[1].split("\n        }", 1)[0]
MIGRATION = re.findall(r'db\.execSQL\("([^"\n]+)"\)', V6_BLOCK)
CREATE = next(sql for sql in re.findall(r'db\.execSQL\("([^"\n]+)"\)', SOURCE)
              if sql.startswith("CREATE TABLE measures ("))
OLD_CREATE = ("CREATE TABLE measures (id INTEGER PRIMARY KEY AUTOINCREMENT, "
              "date TEXT NOT NULL, kg REAL NOT NULL, waist REAL NOT NULL)")


def old_database(rows=()):
    db = sqlite3.connect(":memory:")
    db.execute(OLD_CREATE)
    db.executemany("INSERT INTO measures VALUES (?,?,?,?)", rows)
    db.commit()
    return db


def migrate(db):
    with db:
        for sql in MIGRATION:
            db.execute(sql)


class OptionalCircumferenceMigrationTest(unittest.TestCase):
    def test_all_old_values_and_ids_survive(self):
        rows = [(2, "2026-10-12", 81.7, 88.0), (19, "2026-10-19", 83.1, 90.0)]
        db = old_database(rows)
        migrate(db)
        self.assertEqual(rows, db.execute("SELECT * FROM measures ORDER BY id").fetchall())
        db.execute("INSERT INTO measures (date,kg) VALUES ('2026-10-20',83.0)")
        self.assertGreater(db.execute("SELECT MAX(id) FROM measures").fetchone()[0], 19)
        self.assertEqual("ok", db.execute("PRAGMA integrity_check").fetchone()[0])

    def test_empty_database_upgrades(self):
        db = old_database()
        migrate(db)
        self.assertEqual([], db.execute("SELECT * FROM measures").fetchall())
        db.execute("INSERT INTO measures (date,kg) VALUES ('2026-10-20',83.0)")
        self.assertIsNone(db.execute("SELECT waist FROM measures").fetchone()[0])

    def test_daily_weights_do_not_create_fake_circumferences(self):
        db = old_database([(1, "2026-10-19", 83.1, 90.0)])
        migrate(db)
        db.executemany("INSERT INTO measures (date,kg,waist) VALUES (?,?,?)", [
            ("2026-10-20", 83.0, None), ("2026-10-21", 82.9, None),
            ("2026-10-22", 83.2, None), ("2026-10-26", 83.2, 90.2)])
        self.assertEqual(5, db.execute("SELECT COUNT(*) FROM measures").fetchone()[0])
        self.assertEqual([(90.0,), (90.2,)], db.execute(
            "SELECT waist FROM measures WHERE waist IS NOT NULL ORDER BY date,id").fetchall())

    def test_circumference_can_be_added_and_cleared_later(self):
        db = old_database([(1, "2026-10-19", 83.1, 90.0)])
        migrate(db)
        db.execute("UPDATE measures SET waist=NULL WHERE id=1")
        self.assertIsNone(db.execute("SELECT waist FROM measures").fetchone()[0])
        db.execute("UPDATE measures SET waist=89.5 WHERE id=1")
        self.assertEqual((1, "2026-10-19", 83.1, 89.5),
                         db.execute("SELECT * FROM measures").fetchone())

    def test_fresh_install_allows_optional_circumference(self):
        db = sqlite3.connect(":memory:")
        db.execute(CREATE)
        db.execute("INSERT INTO measures (date,kg) VALUES ('2026-10-20',83.0)")
        self.assertEqual((83.0, None), db.execute("SELECT kg,waist FROM measures").fetchone())
        columns = {r[1]: r for r in db.execute("PRAGMA table_info(measures)")}
        self.assertEqual(1, columns["kg"][3])
        self.assertEqual(0, columns["waist"][3])

    def test_other_app_tables_are_preserved(self):
        db = old_database([(1, "2026-10-19", 83.1, 90.0)])
        sqls = re.findall(r'db\.execSQL\("([^"\n]+)"\)', SOURCE)
        tables = {}
        for sql in sqls:
            match = re.match(r"CREATE TABLE (?:IF NOT EXISTS )?(\w+)", sql)
            if match and not match[1].startswith("measures"):
                db.execute(sql)
                tables[match[1]] = None
        db.execute("INSERT INTO sessions (id,day,created,finished) VALUES (7,'mon',1000,2000)")
        db.execute("INSERT INTO workout_sets (id,session,exercise,set_number,weight,reps,done) "
                   "VALUES (21,7,'row',1,'25','12',1)")
        db.execute("INSERT INTO runs (date,km,seconds,note) VALUES ('2026-10-15',7.65,2620,'test')")
        db.execute("INSERT INTO hiit (date,minutes,rounds,work_seconds,rest_seconds) "
                   "VALUES ('2026-10-17',20,10,30,90)")
        db.execute("INSERT INTO plan_days VALUES ('custom','VL','Vlastní',0)")
        db.execute("INSERT INTO plan_exercises (id,day_id,name,sets,unit,position) "
                   "VALUES ('row','custom','Přítahy',4,'KG',0)")
        db.commit()
        before = {t: db.execute(f'SELECT * FROM "{t}"').fetchall() for t in tables}
        migrate(db)
        after = {t: db.execute(f'SELECT * FROM "{t}"').fetchall() for t in tables}
        self.assertEqual(before, after)
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())


if __name__ == "__main__":
    unittest.main(verbosity=2)
