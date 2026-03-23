alter table public.voiding_events
  add column if not exists urgency integer null;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'voiding_events_urgency_check'
  ) then
    alter table public.voiding_events
      add constraint voiding_events_urgency_check
      check (urgency is null or urgency between 1 and 5);
  end if;
end $$;
