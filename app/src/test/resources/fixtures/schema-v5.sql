-- Exact schema statements from the deployed v0.6 LocalStore.kt (database version 5).
-- Source SHA-256: 3cb722e520479f0367407c5688c1b96dae65a12197eb660ec34e5e91271c8a19
-- Synthetic test rows are inserted by LocalStoreCompatibilityTest; no user data.

CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, created INTEGER NOT NULL, finished INTEGER, duration_seconds INTEGER);
CREATE TABLE workout_sets (id INTEGER PRIMARY KEY AUTOINCREMENT, session INTEGER NOT NULL, exercise TEXT NOT NULL, set_number INTEGER NOT NULL, weight TEXT NOT NULL DEFAULT '', reps TEXT NOT NULL DEFAULT '', done INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(session) REFERENCES sessions(id) ON DELETE CASCADE);
CREATE INDEX sets_session ON workout_sets(session);
CREATE TABLE runs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, km REAL NOT NULL, seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '');
CREATE TABLE hiit (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, minutes INTEGER NOT NULL, rounds INTEGER NOT NULL, work_seconds INTEGER NOT NULL, rest_seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '', km REAL, warmup_minutes INTEGER, cooldown_minutes INTEGER, work_resistance INTEGER, rest_resistance INTEGER);
CREATE TABLE measures (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL NOT NULL, waist REAL NOT NULL);
CREATE TABLE IF NOT EXISTS plan_days (id TEXT PRIMARY KEY NOT NULL, short TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS plan_exercises (id TEXT PRIMARY KEY NOT NULL, day_id TEXT NOT NULL, name TEXT NOT NULL, sets INTEGER NOT NULL, unit TEXT NOT NULL, position INTEGER NOT NULL, partner_id TEXT, note TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS exercises_day_position ON plan_exercises(day_id, position);
