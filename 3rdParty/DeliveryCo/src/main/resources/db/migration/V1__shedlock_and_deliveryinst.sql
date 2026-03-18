-- ShedLock table (PostgreSQL)
create table if not exists public.shedlock (
  name text primary key,
  lock_until timestamptz not null,
  locked_at  timestamptz not null,
  locked_by  text not null
);

