alter table public.voiding_events
  add column if not exists has_incontinence boolean not null default false;
