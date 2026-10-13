-- Bolso Inteligente IoT
-- Estado actual del bolso y eventos de seguridad para Supabase Realtime.

create extension if not exists pgcrypto;

create table if not exists public.bag_status (
    bag_id text primary key check (char_length(bag_id) between 1 and 64),
    armed boolean not null,
    bag_open boolean not null,
    keys_present boolean not null,
    keys_rfid_verified boolean not null,
    wallet_present boolean not null,
    wallet_rfid_verified boolean not null,
    alarm_type text not null check (
        alarm_type in ('none', 'missing_object', 'opening_detected')
    ),
    alarm_latched boolean not null default false,
    keys_echo_us bigint check (keys_echo_us is null or keys_echo_us >= 0),
    wallet_echo_us bigint check (wallet_echo_us is null or wallet_echo_us >= 0),
    device_timestamp_ms bigint check (
        device_timestamp_ms is null or device_timestamp_ms >= 0
    ),
    updated_at timestamptz not null default now()
);

create table if not exists public.bag_events (
    id uuid primary key default gen_random_uuid(),
    bag_id text not null references public.bag_status(bag_id) on delete cascade,
    event_type text not null,
    title text not null,
    description text not null,
    severity text not null check (
        severity in ('info', 'success', 'warning', 'critical')
    ),
    created_at timestamptz not null default now()
);

create index if not exists bag_events_bag_id_created_at_idx
    on public.bag_events (bag_id, created_at desc);

create or replace function public.set_bag_status_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists set_bag_status_updated_at on public.bag_status;
create trigger set_bag_status_updated_at
before update on public.bag_status
for each row
execute function public.set_bag_status_updated_at();

alter table public.bag_status enable row level security;
alter table public.bag_events enable row level security;

revoke all on table public.bag_status from anon, authenticated;
revoke all on table public.bag_events from anon, authenticated;
grant usage on schema public to anon, authenticated;
grant select on table public.bag_status to anon, authenticated;
grant select on table public.bag_events to anon, authenticated;

drop policy if exists "demo bag status is readable" on public.bag_status;
create policy "demo bag status is readable"
on public.bag_status
for select
to anon, authenticated
using (bag_id = 'bolso-demo-001');

drop policy if exists "demo bag events are readable" on public.bag_events;
create policy "demo bag events are readable"
on public.bag_events
for select
to anon, authenticated
using (bag_id = 'bolso-demo-001');

do $$
begin
    alter publication supabase_realtime add table public.bag_status;
exception
    when duplicate_object then null;
end;
$$;

do $$
begin
    alter publication supabase_realtime add table public.bag_events;
exception
    when duplicate_object then null;
end;
$$;
