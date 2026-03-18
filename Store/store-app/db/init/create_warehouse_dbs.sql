-- Create additional databases for warehouse services on first container init
-- This file is mounted into Postgres container at /docker-entrypoint-initdb.d
-- and will run only when PGDATA is empty (first init)

DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'warehouse1') THEN
      PERFORM dblink_exec('dbname=' || current_database(), 'CREATE DATABASE warehouse1 OWNER ' || current_user);
   END IF;
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'warehouse2') THEN
      PERFORM dblink_exec('dbname=' || current_database(), 'CREATE DATABASE warehouse2 OWNER ' || current_user);
   END IF;
END$$;

-- Fallback simple create (for environments without dblink); errors ignored if exist
-- CREATE DATABASE warehouse1 OWNER admin;
-- CREATE DATABASE warehouse2 OWNER admin;


