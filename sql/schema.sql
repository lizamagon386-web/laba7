-- ============================================================
--  Лабораторная работа №7 — схема базы данных (PostgreSQL)
--  Запуск:  psql -h pg -U <логин> -d studs -f sql/schema.sql
-- ============================================================

-- ------------------------------------------------------------
--  Пользователи
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS user_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY DEFAULT nextval('user_id_seq'),
    name            VARCHAR(64)  NOT NULL UNIQUE,
    -- SHA-1 в hex-виде — ровно 40 символов
    password_digest CHAR(40)     NOT NULL,
    salt            VARCHAR(32)  NOT NULL
);

ALTER SEQUENCE user_id_seq OWNED BY users.id;

-- ------------------------------------------------------------
--  Коллекция фильмов
--  id генерируется средствами БД — последовательностью movie_id_seq
--  (требование п.2 задания)
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS movie_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS movies (
    id                      INTEGER PRIMARY KEY DEFAULT nextval('movie_id_seq'),

    name                    VARCHAR(255)      NOT NULL CHECK (name <> ''),
    coordinate_x            INTEGER           NOT NULL CHECK (coordinate_x > -93),
    coordinate_y            DOUBLE PRECISION  NOT NULL,
    creation_date           DATE              NOT NULL DEFAULT CURRENT_DATE,
    oscars_count            INTEGER           CHECK (oscars_count IS NULL OR oscars_count >= 0),
    genre                   VARCHAR(32)       NOT NULL,
    mpaa_rating             VARCHAR(32)       NOT NULL,

    -- сценарист (Person) — может отсутствовать целиком
    screenwriter_name       VARCHAR(255),
    screenwriter_weight     INTEGER           CHECK (screenwriter_weight IS NULL OR screenwriter_weight > 0),
    screenwriter_eye_color  VARCHAR(32),
    screenwriter_hair_color VARCHAR(32),

    -- место рождения сценариста (Location) — тоже необязательно
    location_x              BIGINT,
    location_y              DOUBLE PRECISION,
    location_z              INTEGER,
    location_name           VARCHAR(611),

    -- владелец объекта (п.8 задания)
    creator_id              INTEGER           NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

ALTER SEQUENCE movie_id_seq OWNED BY movies.id;

CREATE INDEX IF NOT EXISTS movies_creator_id_idx ON movies (creator_id);
