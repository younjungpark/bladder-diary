alter table public.voiding_events
  add column if not exists memo_ciphertext text null;

alter table public.voiding_events
  add column if not exists memo_encryption text not null default 'NONE';

alter table public.voiding_events
  drop column if exists memo;

create table if not exists public.user_e2ee_keys (
  user_id uuid primary key references auth.users(id) on delete cascade,
  kdf text not null,
  kdf_salt text not null,
  kdf_params jsonb not null,
  wrapped_dek text not null,
  key_version int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create or replace function public.touch_user_e2ee_keys_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_user_e2ee_keys_updated_at on public.user_e2ee_keys;
create trigger trg_user_e2ee_keys_updated_at
before update on public.user_e2ee_keys
for each row
execute function public.touch_user_e2ee_keys_updated_at();

alter table public.user_e2ee_keys enable row level security;

drop policy if exists "user_e2ee_keys_select_own" on public.user_e2ee_keys;
create policy "user_e2ee_keys_select_own"
  on public.user_e2ee_keys
  for select
  using (auth.uid() = user_id);

drop policy if exists "user_e2ee_keys_insert_own" on public.user_e2ee_keys;
create policy "user_e2ee_keys_insert_own"
  on public.user_e2ee_keys
  for insert
  with check (auth.uid() = user_id);

drop policy if exists "user_e2ee_keys_update_own" on public.user_e2ee_keys;
create policy "user_e2ee_keys_update_own"
  on public.user_e2ee_keys
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
