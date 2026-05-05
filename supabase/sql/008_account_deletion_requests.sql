create table if not exists public.account_deletion_requests (
  request_id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  email text null,
  provider text null,
  account_summary text not null,
  status text not null default 'PENDING',
  operator_note text null,
  requested_at timestamptz not null default now(),
  processed_at timestamptz null
);

create index if not exists idx_account_deletion_requests_status_requested_at
  on public.account_deletion_requests(status, requested_at desc);

alter table public.account_deletion_requests enable row level security;

drop policy if exists "account_deletion_requests_insert_own" on public.account_deletion_requests;
create policy "account_deletion_requests_insert_own"
  on public.account_deletion_requests
  for insert
  with check (auth.uid() = user_id);
