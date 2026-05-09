alter table public.voiding_events
  add column if not exists record_ciphertext text null;

alter table public.voiding_events
  add column if not exists record_encryption text not null default 'NONE';
