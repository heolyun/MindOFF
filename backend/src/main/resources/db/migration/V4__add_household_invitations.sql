create table household_invitations (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    email varchar(320) not null,
    invited_by uuid not null references app_users(id),
    token varchar(100) not null unique,
    status varchar(20) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    accepted_at timestamp with time zone
);

create index ix_household_invitations_household on household_invitations(household_id, status, created_at);
create index ix_household_invitations_email on household_invitations(email, status);
