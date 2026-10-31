-- AB Cricket: private circles / rooms foundation
-- Run once in Supabase SQL Editor. Uses Supabase Auth users as identities.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  avatar_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.cricket_circles (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(trim(name)) between 2 and 40),
  description text not null default '',
  owner_id uuid not null references auth.users(id) on delete cascade,
  invite_code text not null unique default upper(encode(gen_random_bytes(5), 'hex')),
  created_at timestamptz not null default now()
);

create table if not exists public.circle_members (
  circle_id uuid not null references public.cricket_circles(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','admin','member')),
  joined_at timestamptz not null default now(),
  primary key (circle_id, user_id)
);

create table if not exists public.circle_invites (
  id uuid primary key default gen_random_uuid(),
  circle_id uuid not null references public.cricket_circles(id) on delete cascade,
  created_by uuid not null references auth.users(id) on delete cascade,
  invite_code text not null unique default upper(encode(gen_random_bytes(5), 'hex')),
  expires_at timestamptz,
  max_uses integer check (max_uses is null or max_uses > 0),
  use_count integer not null default 0 check (use_count >= 0),
  created_at timestamptz not null default now()
);

create index if not exists circle_members_user_id_idx on public.circle_members(user_id);
create index if not exists circle_invites_circle_id_idx on public.circle_invites(circle_id);

-- Create a profile whenever a new Supabase Auth user signs up.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name, avatar_url)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', ''),
    new.raw_user_meta_data ->> 'avatar_url'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
after insert on auth.users
for each row execute procedure public.handle_new_auth_user();

-- Every newly created circle automatically adds its creator as owner.
create or replace function public.add_circle_owner()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.circle_members(circle_id, user_id, role)
  values (new.id, new.owner_id, 'owner')
  on conflict (circle_id, user_id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_circle_created_add_owner on public.cricket_circles;
create trigger on_circle_created_add_owner
after insert on public.cricket_circles
for each row execute procedure public.add_circle_owner();

-- SECURITY DEFINER helper avoids recursive RLS checks on circle_members.
create or replace function public.is_circle_member(p_circle_id uuid, p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $
  select exists (
    select 1 from public.circle_members
    where circle_id = p_circle_id and user_id = p_user_id
  );
$;
revoke all on function public.is_circle_member(uuid, uuid) from public;
grant execute on function public.is_circle_member(uuid, uuid) to authenticated;

alter table public.profiles enable row level security;
alter table public.cricket_circles enable row level security;
alter table public.circle_members enable row level security;
alter table public.circle_invites enable row level security;

-- Users can view/update only their own profile.
drop policy if exists "profiles_select_self" on public.profiles;
drop policy if exists "profiles_select_self_or_roommates" on public.profiles;
create policy "profiles_select_self_or_roommates" on public.profiles
for select to authenticated using (
  id = (select auth.uid()) or exists (
    select 1 from public.circle_members m
    where m.user_id = profiles.id
      and public.is_circle_member(m.circle_id, (select auth.uid()))
  )
);
drop policy if exists "profiles_update_self" on public.profiles;
create policy "profiles_update_self" on public.profiles
for update to authenticated using (id = (select auth.uid())) with check (id = (select auth.uid()));

-- Circle members can view their circles. Only the owner can create/delete;
-- owner/admin can update the room details.
drop policy if exists "circles_select_members" on public.cricket_circles;
create policy "circles_select_members" on public.cricket_circles
for select to authenticated using (
  public.is_circle_member(id, (select auth.uid()))
);
drop policy if exists "circles_insert_owner" on public.cricket_circles;
create policy "circles_insert_owner" on public.cricket_circles
for insert to authenticated with check (owner_id = (select auth.uid()));
drop policy if exists "circles_update_admins" on public.cricket_circles;
create policy "circles_update_admins" on public.cricket_circles
for update to authenticated using (
  exists (select 1 from public.circle_members m
          where m.circle_id = id and m.user_id = (select auth.uid())
            and m.role in ('owner','admin'))
) with check (
  exists (select 1 from public.circle_members m
          where m.circle_id = id and m.user_id = (select auth.uid())
            and m.role in ('owner','admin'))
);
drop policy if exists "circles_delete_owner" on public.cricket_circles;
create policy "circles_delete_owner" on public.cricket_circles
for delete to authenticated using (owner_id = (select auth.uid()));

-- Members can see the member list of circles they belong to.
drop policy if exists "members_select_circle_members" on public.circle_members;
create policy "members_select_circle_members" on public.circle_members
for select to authenticated using (
  public.is_circle_member(circle_id, (select auth.uid()))
);

-- Only room owners/admins can create invite codes. Members can read invite
-- metadata for their own room; joining is performed through the RPC below.
drop policy if exists "invites_select_room_members" on public.circle_invites;
create policy "invites_select_room_members" on public.circle_invites
for select to authenticated using (
  public.is_circle_member(circle_id, (select auth.uid()))
);
drop policy if exists "invites_insert_room_admins" on public.circle_invites;
create policy "invites_insert_room_admins" on public.circle_invites
for insert to authenticated with check (
  created_by = (select auth.uid()) and
  exists (select 1 from public.circle_members m
          where m.circle_id = circle_id and m.user_id = (select auth.uid())
            and m.role in ('owner','admin'))
);

-- Join a room with its invite code. The code is checked server-side so users
-- cannot add themselves to arbitrary rooms by guessing a room UUID.
create or replace function public.join_circle_by_code(p_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_invite public.circle_invites%rowtype;
  v_circle_id uuid;
begin
  if auth.uid() is null then
    raise exception 'You must be signed in to join a room';
  end if;

  select * into v_invite
  from public.circle_invites
  where upper(invite_code) = upper(trim(p_code))
    and (expires_at is null or expires_at > now())
    and (max_uses is null or use_count < max_uses)
  limit 1
  for update;

  if found then
    v_circle_id := v_invite.circle_id;
    update public.circle_invites set use_count = use_count + 1 where id = v_invite.id;
  else
    select id into v_circle_id
    from public.cricket_circles
    where upper(invite_code) = upper(trim(p_code))
    limit 1;
  end if;

  if v_circle_id is null then
    raise exception 'Invalid or expired room invite code';
  end if;

  insert into public.circle_members(circle_id, user_id, role)
  values (v_circle_id, auth.uid(), 'member')
  on conflict (circle_id, user_id) do nothing;

  return v_circle_id;
end;
$$;

revoke all on function public.join_circle_by_code(text) from public;
grant execute on function public.join_circle_by_code(text) to authenticated;

grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.cricket_circles to authenticated;
grant select on public.circle_members to authenticated;
grant select, insert on public.circle_invites to authenticated;
