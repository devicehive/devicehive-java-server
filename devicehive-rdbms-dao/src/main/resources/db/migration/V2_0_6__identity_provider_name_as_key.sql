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
create temp table identity_temp_migration as
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
  select name, api_endpoint, verification_endpoint, token_endpoint from identity_temp_migration;

-- done