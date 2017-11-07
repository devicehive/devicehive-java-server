---
-- #%L
-- DeviceHive Dao RDBMS Implementation
-- %%
-- Copyright (C) 2017 DataArt
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
-- PostgreSQL database creation script for DeviceHive
CREATE TABLE plugin (
  id                    BIGSERIAL        NOT NULL,
  name                  VARCHAR(128)     NOT NULL,
  description           VARCHAR(128)     NULL,
  topic_name            VARCHAR(128)     NOT NULL,
  health_check_url      VARCHAR(128)     NOT NULL,
  status                INT              NOT NULL,
  subscription_id       BIGSERIAL        NOT NULL,
  user_id               BIGSERIAL        NOT NULL,
  parameters            TEXT             NULL,
  entity_version        BIGINT           NOT NULL DEFAULT 0
);

ALTER TABLE plugin ADD CONSTRAINT plugin_pk PRIMARY KEY (id);
ALTER TABLE plugin ADD CONSTRAINT plugin_name_unique UNIQUE (name);
ALTER TABLE plugin ADD CONSTRAINT plugin_topic_unique UNIQUE (topic_name);
