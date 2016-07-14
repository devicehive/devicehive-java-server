--- create separate table to keep all the
insert into #configuration_temp_migration (name, value)
  select name, value
  from configuration;

drop table configuration;

create table configuration (
  name           varchar(32)  not null,
  value          varchar(128) not null
);

-- add primary key
alter table configuration add constraint configuration_pk primary key (name);

-- copy values back into the provider table
insert into configuration (name, value)
  select name, value from #configuration_temp_migration;

-- done