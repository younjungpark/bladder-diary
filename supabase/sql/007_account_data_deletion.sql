drop policy if exists "voiding_events_delete_own" on public.voiding_events;
create policy "voiding_events_delete_own"
  on public.voiding_events
  for delete
  using (auth.uid() = user_id);

drop policy if exists "user_e2ee_keys_delete_own" on public.user_e2ee_keys;
create policy "user_e2ee_keys_delete_own"
  on public.user_e2ee_keys
  for delete
  using (auth.uid() = user_id);
