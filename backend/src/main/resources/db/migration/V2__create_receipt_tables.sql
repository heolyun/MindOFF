create table receipts (
    id uuid primary key,
    household_id uuid not null references households(id) on delete cascade,
    uploaded_by uuid not null references app_users(id),
    merchant_name varchar(160) not null,
    purchased_at date not null,
    total_amount numeric(14, 2) not null,
    image_name varchar(300),
    image_content_type varchar(120),
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    confirmed_at timestamp with time zone
);

create table receipt_lines (
    id uuid primary key,
    receipt_id uuid not null references receipts(id) on delete cascade,
    name varchar(160) not null,
    quantity numeric(10, 2) not null,
    unit_price numeric(14, 2) not null,
    line_total numeric(14, 2) not null,
    target_type varchar(30) not null,
    expires_at date
);

create index ix_receipts_household_status on receipts(household_id, status, created_at);
create index ix_receipt_lines_receipt on receipt_lines(receipt_id);
