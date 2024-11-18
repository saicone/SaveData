-- create:data_table

CREATE TABLE "{table_name}" (
  "id"         SERIAL PRIMARY KEY NOT NULL,
  "user"       VARCHAR(36)        NOT NULL,
  "type"       VARCHAR(255),
  "key"        VARCHAR(255)       NOT NULL,
  "value"      TEXT               NOT NULL,
  "expiration" BIGINT
);
CREATE INDEX "{table_name}_user" ON "{table_name}" ("user");

-- select:data

SELECT {column_set} FROM "{table_name}" WHERE "user" = ?;

-- select:data_entry

SELECT {column_set} FROM "{table_name}" WHERE "user" = ? AND "key" = ?;

-- select:top_entry

SELECT {column_set} FROM "{table_name}" WHERE "key" = ?;

-- insert:data

INSERT INTO "{table_name}" (
  "user",
  "type",
  "key",
  "value",
  "expiration"
) VALUES (?, ?, ?, ?, ?);

-- update:data

UPDATE "{table_name}" SET {column_set} WHERE "id" = ?;

-- delete:data

DELETE FROM "{table_name}" WHERE {column_set};