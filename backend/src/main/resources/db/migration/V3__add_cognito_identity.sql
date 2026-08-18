alter table app_users add column external_subject varchar(160);
alter table app_users add constraint uq_app_users_external_subject unique (external_subject);
