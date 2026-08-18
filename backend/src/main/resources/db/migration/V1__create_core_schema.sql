create table app_users (
    id uuid primary key,
    email varchar(320) not null unique,
    name varchar(100) not null,
    auth_provider varchar(40) not null default 'DEV',
    created_at timestamp with time zone not null
);

create table households (
    id uuid primary key,
    name varchar(120) not null,
    owner_id uuid not null references app_users(id),
    created_at timestamp with time zone not null
);

create table household_members (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    user_id uuid not null references app_users(id) on delete cascade,
    role varchar(20) not null,
    created_at timestamp with time zone not null,
    constraint uq_household_member unique (household_id, user_id)
);

create table fridge_items (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    name varchar(160) not null,
    purchased_at date not null,
    expires_at date,
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create table household_items (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    name varchar(160) not null,
    purchased_at date not null,
    finished_at date,
    predicted_days integer,
    repeat_purchase boolean not null,
    purchase_url varchar(1000),
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create table usage_cycles (
    id uuid primary key,
    household_item_id uuid not null references household_items(id) on delete cascade,
    started_at date not null,
    finished_at date not null,
    duration_days integer not null,
    created_at timestamp with time zone not null
);

create table need_list_items (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    source_type varchar(30) not null,
    source_id uuid,
    name varchar(160) not null,
    purchase_url varchar(1000),
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone
);

create table subscriptions (
    id uuid primary key,
    user_id uuid not null references app_users(id) on delete cascade,
    name varchar(160) not null,
    amount numeric(14, 2) not null,
    billing_cycle varchar(20) not null,
    next_billing_at date,
    trial_end_at date,
    management_url varchar(1000),
    shared boolean not null,
    created_at timestamp with time zone not null
);

create index ix_members_user on household_members(user_id);
create index ix_fridge_household_expiry on fridge_items(household_id, status, expires_at);
create index ix_household_items_status on household_items(household_id, status);
create index ix_need_list_status on need_list_items(household_id, status);
create index ix_subscriptions_user on subscriptions(user_id, next_billing_at);
