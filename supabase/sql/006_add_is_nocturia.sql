alter table public.voiding_events
  add column if not exists is_nocturia boolean not null default false;
