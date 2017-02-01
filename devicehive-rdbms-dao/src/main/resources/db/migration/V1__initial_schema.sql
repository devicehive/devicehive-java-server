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
-- PostgreSQL database creation script for DeviceHive
CREATE TABLE network (
  id             BIGSERIAL    NOT NULL,
  name           VARCHAR(128) NOT NULL,
  description    VARCHAR(128) NULL,
  key            VARCHAR(64)  NULL,
  entity_version BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE network ADD CONSTRAINT network_pk PRIMARY KEY (id);
ALTER TABLE network ADD CONSTRAINT network_name_unique UNIQUE (name);

CREATE TABLE "user" (
  id             BIGSERIAL                NOT NULL,
  login          VARCHAR(64)              NOT NULL,
  password_hash  VARCHAR(128)              NOT NULL,
  password_salt  VARCHAR(64)              NOT NULL,
  role           INT                      NOT NULL,
  status         INT                      NOT NULL,
  login_attempts INT                      NOT NULL,
  last_login     TIMESTAMP WITH TIME ZONE NULL,
  entity_version BIGINT                   NOT NULL DEFAULT 0
);

ALTER TABLE "user" ADD CONSTRAINT user_pk PRIMARY KEY (id);
ALTER TABLE "user" ADD CONSTRAINT user_login_unique UNIQUE (login);

CREATE TABLE user_network (
  id             BIGSERIAL NOT NULL,
  user_id        BIGINT    NOT NULL,
  network_id     BIGINT    NOT NULL,
  entity_version BIGINT DEFAULT 0
);

ALTER TABLE user_network ADD CONSTRAINT user_network_pk PRIMARY KEY (id);
ALTER TABLE user_network ADD CONSTRAINT user_network_user_pk FOREIGN KEY (user_id) REFERENCES "user" (id);
ALTER TABLE user_network ADD CONSTRAINT user_network_network_pk FOREIGN KEY (network_id) REFERENCES network (id);

CREATE TABLE device_class (
  id              BIGSERIAL    NOT NULL,
  name            VARCHAR(128) NOT NULL,
  version         VARCHAR(32)  NOT NULL,
  is_permanent    BOOLEAN      NOT NULL,
  offline_timeout INT          NULL,
  data            TEXT         NULL,
  entity_version  BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE device_class ADD CONSTRAINT device_class_pk PRIMARY KEY (id);
ALTER TABLE device_class ADD CONSTRAINT device_class_name_version_unique UNIQUE (name, version);

CREATE TABLE device (
  id              BIGSERIAL    NOT NULL,
  guid            UUID         NOT NULL,
  name            VARCHAR(128) NOT NULL,
  status          VARCHAR(128) NULL,
  network_id      BIGINT       NULL,
  device_class_id BIGINT       NOT NULL,
  key             VARCHAR(64)  NOT NULL,
  data            TEXT         NULL,
  entity_version  BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE device ADD CONSTRAINT device_pk PRIMARY KEY (id);
ALTER TABLE device ADD CONSTRAINT device_network_fk FOREIGN KEY (network_id) REFERENCES network (id) ON DELETE CASCADE;
ALTER TABLE device ADD CONSTRAINT device_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id) ON DELETE CASCADE;
ALTER TABLE device ADD CONSTRAINT device_guid_unique UNIQUE (guid);

CREATE TABLE device_equipment (
  id             BIGSERIAL                NOT NULL,
  code           VARCHAR(128)             NOT NULL,
  timestamp      TIMESTAMP WITH TIME ZONE NOT NULL,
  parameters     TEXT                     NULL,
  device_id      BIGINT                   NOT NULL,
  entity_version BIGINT                   NOT NULL DEFAULT 0
);

ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_pk PRIMARY KEY (id);
ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_device_fk FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE;
ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_device_id_code_unique UNIQUE (device_id, code);
CREATE INDEX device_equipment_device_id_timestamp_idx ON device_equipment (device_id, timestamp);

CREATE TABLE equipment (
  id              BIGSERIAL    NOT NULL,
  name            VARCHAR(128) NOT NULL,
  code            VARCHAR(128) NOT NULL,
  device_class_id BIGINT       NOT NULL,
  type            VARCHAR(128) NOT NULL,
  data            TEXT         NULL,
  entity_version  BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE equipment ADD CONSTRAINT equipment_pk PRIMARY KEY (id);
ALTER TABLE equipment ADD CONSTRAINT equipment_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id) ON DELETE CASCADE;
ALTER TABLE equipment ADD CONSTRAINT equipment_code_device_class_id_unique UNIQUE (code, device_class_id);

CREATE TABLE configuration (
  name           VARCHAR(32)  NOT NULL,
  value          VARCHAR(128) NOT NULL,
  entity_version BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE configuration ADD CONSTRAINT configuration_pk PRIMARY KEY (name);

CREATE VIEW get_timestamp AS
  SELECT
    now() AS timestamp;


-- Reference data for initial database setup:

-- 1. Default users
INSERT INTO "user"
(login, password_hash, password_salt, role, status, login_attempts)
  VALUES
  ('dhadmin', 'DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=', 'sjQbZgcCmFxqTV4CCmGwpIHO', 0, 0, 0);

-- 2. Default device classes
INSERT INTO device_class
(name, version, is_permanent, offline_timeout)
  VALUES
  ('Sample VirtualLed Device', 1.0, FALSE, 600);

-- 3. Default networks
INSERT INTO network
(name, description)
  VALUES
  ('VirtualLed Sample Network', 'A DeviceHive network for VirtualLed sample');

-- 4. Default devices
INSERT INTO device
(guid, name, status, network_id, device_class_id, key)
  VALUES
  ('E50D6085-2ABA-48E9-B1C3-73C673E414BE', 'Sample VirtualLed Device', 'Offline', 1, 1, '05F94BF509C8');