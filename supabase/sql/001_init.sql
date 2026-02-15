create table if not exists public.voiding_events (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  voided_at timestamptz not null,
  local_date date not null,
  client_ref text not null unique,
  created_at timestamptz not null default now(),
  deleted_at timestamptz null
);

create index if not exists idx_voiding_events_user_date
  on public.voiding_events(user_id, local_date);

create index if not exists idx_voiding_events_user_voided_at
  on public.voiding_events(user_id, voided_at desc);

alter table public.voiding_events enable row level security;

drop policy if exists "voiding_events_select_own" on public.voiding_events;
create policy "voiding_events_select_own"
  on public.voiding_events
  for select
  using (auth.uid() = user_id);

drop policy if exists "voiding_events_insert_own" on public.voiding_events;
create policy "voiding_events_insert_own"
  on public.voiding_events
  for insert
  with check (auth.uid() = user_id);

drop policy if exists "voiding_events_update_own" on public.voiding_events;
create policy "voiding_events_update_own"
  on public.voiding_events
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
