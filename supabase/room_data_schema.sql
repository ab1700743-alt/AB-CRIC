-- AB Cricket: room-scoped cricket data
-- Run this AFTER rooms_schema.sql in Supabase SQL Editor.
-- Every room member can read. Only room owner/admin can insert, update or delete.

create or replace function public.has_circle_role(
  p_circle_id uuid,
  p_user_id uuid,
  p_roles text[] default array['owner','admin']::text[]
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.circle_members m
    where m.circle_id = p_circle_id
      and m.user_id = p_user_id
      and m.role = any(p_roles)
  );
$$;
revoke all on function public.has_circle_role(uuid, uuid, text[]) from public;
grant execute on function public.has_circle_role(uuid, uuid, text[]) to authenticated;

create table if not exists public.room_players (
  id uuid primary key default gen_random_uuid(),
  circle_id uuid not null references public.cricket_circles(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 60),
  role text not null default 'Batsman'
    check (role in ('Batsman','Bowler','All-rounder','Wicketkeeper')),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  unique (id, circle_id)
);
create index if not exists room_players_circle_idx on public.room_players(circle_id);

create table if not exists public.room_matches (
  id uuid primary key default gen_random_uuid(),
  circle_id uuid not null references public.cricket_circles(id) on delete cascade,
  title text not null default 'Cricket Match',
  team_one text not null,
  team_two text not null,
  overs_limit integer not null default 20 check (overs_limit between 1 and 100),
  status text not null default 'upcoming'
    check (status in ('upcoming','live','completed','abandoned')),
  scorecard jsonb not null default '{}'::jsonb,
  created_by uuid not null references auth.users(id),
  started_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, circle_id)
);
create index if not exists room_matches_circle_created_idx on public.room_matches(circle_id, created_at desc);
create index if not exists room_matches_circle_status_idx on public.room_matches(circle_id, status);

create table if not exists public.room_ball_events (
  id uuid primary key default gen_random_uuid(),
  circle_id uuid not null,
  match_id uuid not null,
  innings smallint not null check (innings in (1,2)),
  over_number integer not null check (over_number >= 0),
  ball_number integer not null check (ball_number >= 1),
  batter_id uuid,
  bowler_id uuid,
  batter_runs integer not null default 0 check (batter_runs between 0 and 6),
  extras integer not null default 0 check (extras >= 0),
  total_runs integer not null default 0 check (total_runs >= 0),
  wicket_type text,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  foreign key (match_id, circle_id) references public.room_matches(id, circle_id) on delete cascade,
  foreign key (batter_id, circle_id) references public.room_players(id, circle_id),
  foreign key (bowler_id, circle_id) references public.room_players(id, circle_id)
);
create index if not exists room_ball_events_match_idx on public.room_ball_events(match_id, innings, over_number, ball_number);

alter table public.room_players enable row level security;
alter table public.room_matches enable row level security;
alter table public.room_ball_events enable row level security;

drop policy if exists "room_players_read_members" on public.room_players;
create policy "room_players_read_members" on public.room_players for select to authenticated
using (public.is_circle_member(circle_id, (select auth.uid())));
drop policy if exists "room_players_admin_insert" on public.room_players;
create policy "room_players_admin_insert" on public.room_players for insert to authenticated
with check (public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_players_admin_update" on public.room_players;
create policy "room_players_admin_update" on public.room_players for update to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())))
with check (public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_players_admin_delete" on public.room_players;
create policy "room_players_admin_delete" on public.room_players for delete to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())));

drop policy if exists "room_matches_read_members" on public.room_matches;
create policy "room_matches_read_members" on public.room_matches for select to authenticated
using (public.is_circle_member(circle_id, (select auth.uid())));
drop policy if exists "room_matches_admin_insert" on public.room_matches;
create policy "room_matches_admin_insert" on public.room_matches for insert to authenticated
with check (created_by = (select auth.uid()) and public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_matches_admin_update" on public.room_matches;
create policy "room_matches_admin_update" on public.room_matches for update to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())))
with check (public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_matches_admin_delete" on public.room_matches;
create policy "room_matches_admin_delete" on public.room_matches for delete to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())));

drop policy if exists "room_ball_events_read_members" on public.room_ball_events;
create policy "room_ball_events_read_members" on public.room_ball_events for select to authenticated
using (public.is_circle_member(circle_id, (select auth.uid())));
drop policy if exists "room_ball_events_admin_insert" on public.room_ball_events;
create policy "room_ball_events_admin_insert" on public.room_ball_events for insert to authenticated
with check (created_by = (select auth.uid()) and public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_ball_events_admin_update" on public.room_ball_events;
create policy "room_ball_events_admin_update" on public.room_ball_events for update to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())))
with check (public.has_circle_role(circle_id, (select auth.uid())));
drop policy if exists "room_ball_events_admin_delete" on public.room_ball_events;
create policy "room_ball_events_admin_delete" on public.room_ball_events for delete to authenticated
using (public.has_circle_role(circle_id, (select auth.uid())));

grant select, insert, update, delete on public.room_players to authenticated;
grant select, insert, update, delete on public.room_matches to authenticated;
grant select, insert, update, delete on public.room_ball_events to authenticated;
