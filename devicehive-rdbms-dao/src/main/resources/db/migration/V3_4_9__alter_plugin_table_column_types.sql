---
-- #%L
-- DeviceHive Dao RDBMS Implementation
-- %%
-- Copyright (C) 2016 - 2017 DataArt
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

/*
ALTER TABLE plugin
ALTER COLUMN subscription_id TYPE BIGINT,
ALTER COLUMN user_id TYPE BIGINT;*/

alter table plugin
alter column subscription_id drop default;
drop sequence plugin_subscription_id_seq;

alter table plugin
alter column user_id drop default;
drop sequence plugin_user_id_seq;
