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

CREATE TABLE oauth_client (
  id BIGSERIAL NOT NULL,
  name VARCHAR(128) NOT NULL,
  domain VARCHAR(128) NOT NULL,
  subnet VARCHAR (128),
  redirect_uri VARCHAR(128) NOT NULL,
  oauth_id VARCHAR(32) NOT NULL,
  oauth_secret VARCHAR(32) NOT NULL,
  entity_version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE oauth_client ADD CONSTRAINT oauth_client_pk PRIMARY KEY (id);
ALTER TABLE oauth_client ADD CONSTRAINT oauth_id_unique UNIQUE (oauth_id);
CREATE UNIQUE INDEX oauth_client_idx ON oauth_client (oauth_id);

CREATE TABLE oauth_grant (
  id BIGSERIAL NOT NULL,
  timestamp TIMESTAMP WITH TIME ZONE,
  auth_code VARCHAR(36) NOT NULL,
  client_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  key_id BIGINT NOT NULL,
  type INT NOT NULL,
  access_type INT NOT NULL,
  redirect_uri VARCHAR(128) NOT NULL,
  scope VARCHAR(256) NOT NULL,
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
CREATE UNIQUE INDEX oauth_grant_idx ON oauth_grant (auth_code);

CREATE TABLE oauth_grant_network (
  id BIGSERIAL NOT NULL,
  network_id BIGINT NOT NULL ,
  oauth_grant_id BIGINT NOT NULL,
  entity_version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE oauth_grant_network ADD CONSTRAINT oauth_grant_network_pk PRIMARY KEY (id);
ALTER TABLE oauth_grant_network ADD CONSTRAINT oauth_grant_network_oauth_grant_fk FOREIGN KEY (oauth_grant_id) REFERENCES
  oauth_grant (id) ON DELETE CASCADE;
ALTER TABLE oauth_grant_network ADD CONSTRAINT oauth_grant_network_network_fk FOREIGN KEY (network_id) REFERENCES
  network (id) ON DELETE CASCADE;
CREATE INDEX oauth_grant_network_network_id_idx ON oauth_grant_network (network_id);
CREATE INDEX oauth_grant_network_oauth_grant_id_idx ON oauth_grant_network (oauth_grant_id);

--default: debug features are enabled--
INSERT INTO configuration (name, value)
  VALUES ('debugMode', 'true');