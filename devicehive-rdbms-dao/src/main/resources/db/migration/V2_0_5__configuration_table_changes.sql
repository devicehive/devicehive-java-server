---
-- #%L
-- DeviceHive Dao RDBMS Implementation
-- %%
-- Copyright (C) 2016 DataArt
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
--- create separate table to keep all the
create temp table configuration_temp_migration as select name, value, entity_version from configuration;

drop table configuration;

create table configuration (
  name           varchar(32)  not null,
  value          varchar(128) not null,
  entity_version bigint       not null default 0
);

-- add primary key
alter table configuration add constraint configuration_pk primary key (name);

-- copy values back into the provider table
insert into configuration (name, value, entity_version)
  select name, value, entity_version from configuration_temp_migration;

-- done