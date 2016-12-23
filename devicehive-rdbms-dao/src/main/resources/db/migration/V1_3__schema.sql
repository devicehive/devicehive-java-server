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
ALTER TABLE device
    ALTER COLUMN guid TYPE varchar(48);

CREATE TABLE access_key (
    id BIGSERIAL NOT NULL,
    label VARCHAR(64) NOT NULL,
    key VARCHAR(48) NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE,
    user_id BIGINT NOT NULL,
    entity_version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE access_key ADD CONSTRAINT access_key_pk PRIMARY KEY (id);
ALTER TABLE access_key ADD CONSTRAINT access_key_user_fk FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE
CASCADE;
ALTER TABLE access_key ADD CONSTRAINT access_key_key_unique UNIQUE (key);
CREATE UNIQUE INDEX access_key_key_idx ON access_key (key);

CREATE TABLE access_key_permission (
  id BIGSERIAL NOT NULL,
  access_key_id BIGINT NOT NULL,
  domains TEXT NULL,
  subnets TEXT NULL,
  actions TEXT NULL,
  network_ids TEXT NULL,
  device_guids TEXT NULL,
  entity_version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE access_key_permission ADD CONSTRAINT access_key_permission_pk PRIMARY KEY (id);
ALTER TABLE access_key_permission ADD CONSTRAINT access_key_permission_access_key_fk FOREIGN KEY (access_key_id) REFERENCES
access_key (id) ON DELETE CASCADE;
CREATE INDEX access_key_permission_access_key_id_idx ON access_key_permission(access_key_id);

CREATE TABLE oauth_client (
  id BIGSERIAL NOT NULL,
  name VARCHAR(128) NOT NULL,
  domain VARCHAR(128) NOT NULL,
  subnet VARCHAR (128) NULL,
  redirect_uri VARCHAR(128) NOT NULL,
  oauth_id VARCHAR(32) NOT NULL,
  oauth_secret VARCHAR(32) NOT NULL,
  entity_version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE oauth_client ADD CONSTRAINT oauth_client_pk PRIMARY KEY (id);
ALTER TABLE oauth_client ADD CONSTRAINT oauth_id_unique UNIQUE (oauth_id);
CREATE UNIQUE INDEX oauth_client_oauth_id_idx ON oauth_client (oauth_id);

CREATE TABLE oauth_grant (
  id BIGSERIAL NOT NULL,
  timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  auth_code VARCHAR(36) NULL,
  client_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  key_id BIGINT NOT NULL,
  type INT NOT NULL,
  access_type INT NOT NULL,
  redirect_uri VARCHAR(128) NOT NULL,
  scope VARCHAR(256) NOT NULL,
  network_ids VARCHAR(128) NULL,
  entity_version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE oauth_grant ADD CONSTRAINT oauth_grant_pk PRIMARY KEY (id);
ALTER TABLE oauth_grant ADD CONSTRAINT oauth_grant_oauth_client_fk FOREIGN KEY (client_id) REFERENCES oauth_client
(id) ON DELETE CASCADE;
ALTER TABLE oauth_grant ADD CONSTRAINT oauth_grant_user_fk FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE
CASCADE;
ALTER TABLE oauth_grant ADD CONSTRAINT oauth_grant_access_key_fk FOREIGN KEY (key_id) REFERENCES access_key (id) ON
DELETE CASCADE;
ALTER TABLE oauth_grant ADD CONSTRAINT auth_code_unique UNIQUE (auth_code);
CREATE UNIQUE INDEX oauth_grant_auth_code_idx ON oauth_grant (auth_code);
CREATE INDEX oauth_grant_key_id_idx ON oauth_grant(key_id);
CREATE INDEX oauth_grant_client_id_idx ON oauth_grant(client_id);
CREATE INDEX oauth_grant_user_id_idx ON oauth_grant(user_id);

--default: debug features are enabled--
INSERT INTO configuration (name, value)
  VALUES ('debugMode', 'true');

--Indexes
CREATE UNIQUE INDEX device_guid_idx ON device(guid);
CREATE INDEX device_network_id_idx ON device(network_id);
CREATE UNIQUE INDEX device_class_name_version_idx ON device_class(name, version);
CREATE INDEX device_equipment_device_id_code_idx ON device_equipment(device_id, code);
CREATE INDEX equipment_device_class_id_idx ON equipment(device_class_id);
CREATE UNIQUE INDEX network_name_idx ON network(name);
CREATE UNIQUE INDEX user_login_idx ON "user"(login);
CREATE INDEX user_network_user_id_network_id_idx ON user_network(user_id, network_id);
CREATE INDEX user_network_network_id_user_id_idx ON user_network(network_id, user_id);



ALTER TABLE configuration DROP CONSTRAINT configuration_pk;

ALTER TABLE configuration
  add column id BIGSERIAL not null;

ALTER TABLE configuration ADD CONSTRAINT configuration_pk PRIMARY KEY (id);
ALTER TABLE configuration ADD CONSTRAINT configuration_name_unique UNIQUE (name);

CREATE TABLE identity_provider (
id BIGSERIAL NOT NULL,
name VARCHAR(64) NOT NULL,
api_endpoint VARCHAR(128),
verification_endpoint VARCHAR(128),
entity_version BIGINT NOT NULL DEFAULT 0);

ALTER TABLE identity_provider ADD CONSTRAINT identity_provider_pk PRIMARY KEY (id);

INSERT INTO identity_provider(id, name) VALUES (0,'devicehive');
INSERT INTO identity_provider(id, name, api_endpoint, verification_endpoint) VALUES (1,'google', 'https://www.googleapis.com/plus/v1/people/me', 'https://www.googleapis.com/oauth2/v1/tokeninfo');
INSERT INTO identity_provider(id, name, api_endpoint, verification_endpoint) VALUES (2,'facebook', 'https://graph.facebook.com/me', 'https://graph.facebook.com/app');
INSERT INTO identity_provider(id, name, api_endpoint) VALUES (3,'github', 'https://api.github.com/user/emails');

alter table "user" add column google_login VARCHAR(64) UNIQUE;
alter table "user" add column facebook_login VARCHAR(64) UNIQUE;
alter table "user" add column github_login VARCHAR(64) UNIQUE;

ALTER TABLE "user" ALTER COLUMN password_hash  DROP NOT NULL;
ALTER TABLE "user" ALTER COLUMN password_salt  DROP NOT NULL;

ALTER TABLE access_key ADD CONSTRAINT access_key_label_user_unique UNIQUE (label, user_id);

INSERT INTO configuration (name, value) VALUES ('google.identity.allowed', 'false');
INSERT INTO configuration (name, value) VALUES ('facebook.identity.allowed', 'false');
INSERT INTO configuration (name, value) VALUES ('github.identity.allowed', 'false');

CREATE TABLE "EJB__TIMER__TBL" (
"CREATIONTIMERAW"      BIGINT        NOT NULL,
"BLOB"                 BYTEA,
"TIMERID"              VARCHAR(255)  NOT NULL,
"CONTAINERID"          BIGINT        NOT NULL,
"OWNERID"              VARCHAR(255)  NULL,
"STATE"                INTEGER       NOT NULL,
"PKHASHCODE"           INTEGER       NOT NULL,
"INTERVALDURATION"     BIGINT        NOT NULL,
"INITIALEXPIRATIONRAW" BIGINT        NOT NULL,
"LASTEXPIRATIONRAW"    BIGINT        NOT NULL,
"SCHEDULE"             VARCHAR(255)  NULL,
"APPLICATIONID"        BIGINT        NOT NULL,
CONSTRAINT "PK_EJB__TIMER__TBL" PRIMARY KEY ("TIMERID")
);

ALTER TABLE identity_provider ADD COLUMN token_endpoint VARCHAR(128);

UPDATE identity_provider SET token_endpoint='https://www.googleapis.com/oauth2/v3/token' WHERE id = 1;
UPDATE identity_provider SET token_endpoint='https://graph.facebook.com/oauth/access_token' WHERE id = 2;
UPDATE identity_provider SET token_endpoint='https://github.com/login/oauth/access_token' WHERE id = 3;

ALTER TABLE access_key DROP CONSTRAINT access_key_label_user_unique;
ALTER TABLE access_key ADD COLUMN type INT NOT NULL DEFAULT 0;
INSERT INTO configuration (name, value) VALUES ('session.timeout', '1200000');

INSERT INTO configuration (name, value) VALUES ('websocket.ping.timeout', '120000');
INSERT INTO configuration (name, value) VALUES ('cassandra.rest.endpoint', 'http://127.0.0.1:8080/cassandra');
INSERT INTO configuration (name, value) VALUES ('user.login.lastTimeout', '1000');

CREATE OR REPLACE FUNCTION insert_test_devices(first_index INT, last_index INT) RETURNS void AS $BODY$
DECLARE
idd text;
BEGIN
FOR r IN first_index..last_index LOOP
select to_char(r, '09999999') into idd;
     INSERT INTO device (guid, name, status, network_id, device_class_id, key) VALUES (
replace('aaaaaaaa-aaaa-0000-0-000000a' || idd, ' ', ''), 'Kafka device', 'Offline', 1, 1, 'TK_' || idd || '_A' );
END LOOP;
RETURN;
END
$BODY$
LANGUAGE plpgsql;

INSERT INTO access_key (label, key, expiration_date, user_id) VALUES ('Access Key for dhadmin', '1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=', null, 1);
INSERT INTO access_key_permission (access_key_id) VALUES (1);

ALTER TABLE device ADD COLUMN blocked BOOLEAN;
CREATE OR REPLACE FUNCTION init_device_blocked_field() RETURNS void as $$
BEGIN
  UPDATE device SET blocked = false WHERE blocked IS NULL;
END;
$$ LANGUAGE plpgsql;
SELECT init_device_blocked_field();

ALTER TABLE "user" ADD COLUMN data TEXT;

