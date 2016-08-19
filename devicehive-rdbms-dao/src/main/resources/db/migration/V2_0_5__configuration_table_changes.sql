--- create separate table to keep all the
create temp table configuration_temp_migration as select name, value, entity_version from configuration;

drop table configuration;

create table configuration (
  name           varchar(32)  not null,
  value          varchar(128) not null,
  entity_version bigint       not null default 0
);

-- add primary partitionKey
alter table configuration add constraint configuration_pk primary partitionKey (name);

-- copy values back into the provider table
insert into configuration (name, value, entity_version)
  select name, value, entity_version from configuration_temp_migration;

-- done