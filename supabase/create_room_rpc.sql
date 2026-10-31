-- Secure room creation: always take owner identity from the verified Supabase JWT.
create or replace function public.create_cricket_circle(p_name text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid;
  v_user_id uuid;
begin
  v_user_id := auth.uid();

  if v_user_id is null then
    raise exception 'Not authenticated';
  end if;

  if p_name is null or length(trim(p_name)) < 2 then
    raise exception 'Room name must be at least 2 characters';
  end if;

  insert into public.cricket_circles (name, description, owner_id)
  values (trim(p_name), '', v_user_id)
  returning id into v_id;

  return v_id;
end;
$$;

revoke all on function public.create_cricket_circle(text) from public;
grant execute on function public.create_cricket_circle(text) to authenticated;
