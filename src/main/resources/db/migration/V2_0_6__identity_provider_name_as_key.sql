--- create separate table to keep all the
insert into #identity_temp_migration (name, api_endpoint, verification_endpoint, token_endpoint)
  select name, api_endpoint, verification_endpoint, token_endpoint
  from identity_provider;

drop table identity_provider;

create table identity_provider (
  name varchar(64) NOT NULL,
  api_endpoint varchar(128),
  verification_endpoint varchar(128),
  token_endpoint varchar(128)
);

-- add primary key
alter table identity_provider add constraint identity_provider_pk primary key (name);

-- copy values back into the provider table
insert into identity_provider (name, api_endpoint, verification_endpoint, token_endpoint)
  select name, api_endpoint, verification_endpoint, token_endpoint from #identity_temp_migration;

-- done