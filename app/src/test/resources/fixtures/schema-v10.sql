-- Installed v0.9 schema, copied before the round-selection migration.
CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, created INTEGER NOT NULL, finished INTEGER, duration_seconds INTEGER);
CREATE TABLE workout_sets (id INTEGER PRIMARY KEY AUTOINCREMENT, session INTEGER NOT NULL, exercise TEXT NOT NULL, set_number INTEGER NOT NULL, weight TEXT NOT NULL DEFAULT '', reps TEXT NOT NULL DEFAULT '', done INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(session) REFERENCES sessions(id) ON DELETE CASCADE);
CREATE INDEX sets_session ON workout_sets(session);
CREATE TABLE runs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, km REAL NOT NULL, seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '');
CREATE TABLE hiit (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, minutes INTEGER NOT NULL, rounds INTEGER NOT NULL, work_seconds INTEGER NOT NULL, rest_seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '', km REAL, warmup_minutes INTEGER, cooldown_minutes INTEGER, work_resistance INTEGER, rest_resistance INTEGER);
CREATE TABLE measures (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL, waist REAL);
CREATE TABLE body_metrics (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL);
CREATE TABLE body_metric_values (id INTEGER PRIMARY KEY AUTOINCREMENT, measurement_id INTEGER NOT NULL, metric_id TEXT NOT NULL, cm REAL NOT NULL CHECK(cm > 0 AND cm <= 300), UNIQUE(measurement_id,metric_id), FOREIGN KEY(measurement_id) REFERENCES measures(id) ON DELETE CASCADE, FOREIGN KEY(metric_id) REFERENCES body_metrics(id));
CREATE TABLE IF NOT EXISTS plan_days (id TEXT PRIMARY KEY NOT NULL, short TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS plan_exercises (id TEXT PRIMARY KEY NOT NULL, day_id TEXT NOT NULL, name TEXT NOT NULL, sets INTEGER NOT NULL, unit TEXT NOT NULL, position INTEGER NOT NULL, partner_id TEXT, note TEXT NOT NULL DEFAULT '');
CREATE INDEX IF NOT EXISTS exercises_day_position ON plan_exercises(day_id, position);
ALTER TABLE plan_exercises ADD COLUMN superset_id TEXT;
ALTER TABLE sessions ADD COLUMN elapsed_offset_seconds INTEGER NOT NULL DEFAULT 0;

ALTER TABLE plan_exercises ADD COLUMN superset_start_round INTEGER NOT NULL DEFAULT 1 CHECK(superset_start_round BETWEEN 0 AND 15);

ALTER TABLE plan_exercises ADD COLUMN load_multiplier INTEGER NOT NULL DEFAULT 1 CHECK(load_multiplier IN (1,2));
