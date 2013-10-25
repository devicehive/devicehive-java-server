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
  subnet VARCHAR (128) NULL,
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
CREATE UNIQUE INDEX oauth_grant_idx ON oauth_grant (auth_code);

--functions
-- select min(timestamp) from device_notification;
CREATE FUNCTION get_Min_Timestamp_From_Notifications()
  RETURNS TIMESTAMP WITH TIME ZONE
AS 'SELECT
      min(timestamp)
    FROM device_notification;'
LANGUAGE SQL
STABLE;

CREATE OR REPLACE FUNCTION get_First_Timestamp(interval_int    INTEGER,
                                               timestamp_value TIMESTAMP WITH TIME ZONE DEFAULT
                                               get_Min_Timestamp_From_Notifications())
  RETURNS TIMESTAMP WITH TIME ZONE
AS 'WITH intervals AS (
    SELECT
      (SELECT
      min(timestamp) :: DATE
       FROM device_notification) + (n || '' seconds'') :: INTERVAL        start_time,
      (SELECT
      min(timestamp) :: DATE
       FROM device_notification) + ((n + $1) || '' seconds'') :: INTERVAL end_time
    FROM generate_series(0, ((SELECT
                                max(timestamp) :: DATE - min(timestamp) :: DATE
                              FROM device_notification) + 1) * 24 * 60 * 60, $1) n)
SELECT
  min
FROM (
SELECT
  min(timestamp)  min,
  row_number()
  OVER (
    ORDER BY f.start_time) as position,
  f.start_time start_time,
  f.end_time end_time
FROM device_notification
  LEFT JOIN intervals f
    ON device_notification.timestamp >= f.start_time AND device_notification.timestamp < f.end_time

GROUP BY f.start_time, f.end_time
ORDER BY f.start_time) as min_timestamp
WHERE min_timestamp.start_time <= $2 AND min_timestamp.end_time > $2;'
LANGUAGE SQL
STABLE;
--default: debug features are enabled--
INSERT INTO configuration (name, value)
  VALUES ('debugMode', 'true');