alter table subscriptions add column household_id uuid references households(id) on delete cascade;

create index ix_subscriptions_household on subscriptions(household_id, shared, next_billing_at);
