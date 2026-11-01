-- AB Cricket: richer ball-by-ball event details
-- Run this in Supabase SQL Editor after room_data_schema.sql.

alter table public.room_ball_events
  add column if not exists event_number integer,
  add column if not exists legal_ball boolean not null default true,
  add column if not exists extra_type text not null default 'none'
    check (extra_type in ('none','wide','no_ball','bye','leg_bye','penalty')),
  add column if not exists is_wicket boolean not null default false,
  add column if not exists striker_name text not null default '',
  add column if not exists non_striker_name text not null default '',
  add column if not exists bowler_name text not null default '',
  add column if not exists commentary text not null default '';

-- Backfill a stable sequence for existing rows before enforcing uniqueness.
with numbered as (
  select id,
         row_number() over (
           partition by match_id, innings
           order by created_at, id
         ) as seq
  from public.room_ball_events
  where event_number is null
)
update public.room_ball_events e
set event_number = numbered.seq
from numbered
where e.id = numbered.id;

alter table public.room_ball_events
  alter column event_number set not null;

create unique index if not exists room_ball_events_sequence_idx
  on public.room_ball_events(match_id, innings, event_number);

create index if not exists room_ball_events_created_idx
  on public.room_ball_events(match_id, created_at);
