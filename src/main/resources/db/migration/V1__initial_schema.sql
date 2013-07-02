-- PostgreSQL database creation script for DeviceHive
CREATE TABLE network (
    id BIGSERIAL NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(128) NULL,
    key VARCHAR(64) NULL
);

ALTER TABLE network ADD CONSTRAINT network_pk PRIMARY KEY (id);

CREATE TABLE "user" (
    id BIGSERIAL NOT NULL,
    login VARCHAR(64) NOT NULL,
    password_hash VARCHAR(48) NOT NULL,
    password_salt VARCHAR(24) NOT NULL,
    role INT NOT NULL,
    status INT NOT NULL,
    login_attempts INT NOT NULL,
    last_login TIMESTAMP NULL
);

ALTER TABLE "user" ADD CONSTRAINT user_pk PRIMARY KEY (id);

CREATE TABLE user_network (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    network_id BIGINT NOT NULL
);

ALTER TABLE user_network ADD CONSTRAINT user_network_pk PRIMARY KEY (id);
ALTER TABLE user_network ADD CONSTRAINT user_network_user_pk FOREIGN KEY (user_id) REFERENCES "user" (id);
ALTER TABLE user_network ADD CONSTRAINT user_network_network_pk FOREIGN KEY (network_id) REFERENCES network (id);

CREATE TABLE device_class (
    id BIGSERIAL NOT NULL,
    name VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL,
    is_permanent BOOLEAN NOT NULL,
    offline_timeout INT NULL,
    data TEXT NULL
);

ALTER TABLE device_class ADD CONSTRAINT device_class_pk PRIMARY KEY (id);

CREATE TABLE device (
    id BIGSERIAL NOT NULL,
    guid UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(128) NULL,
    network_id BIGINT NULL,
    device_class_id BIGINT NOT NULL,
    key VARCHAR(64) NOT NULL,
    data TEXT NULL
);

ALTER TABLE device ADD CONSTRAINT device_pk PRIMARY KEY (id);
ALTER TABLE device ADD CONSTRAINT device_network_fk FOREIGN KEY (network_id) REFERENCES network (id);
ALTER TABLE device ADD CONSTRAINT device_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id);

CREATE TABLE device_command (
    id BIGSERIAL NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    command VARCHAR(128) NOT NULL,
    parameters TEXT NULL,
    lifetime INT NULL,
    flags INT NULL,
    status VARCHAR(128) NULL,
    result TEXT NULL,
    device_id BIGINT NOT NULL,
    user_id BIGINT NULL
);

ALTER TABLE device_command ADD CONSTRAINT device_command_pk PRIMARY KEY (id);
ALTER TABLE device_command ADD CONSTRAINT device_command_device_fk FOREIGN KEY (device_id) REFERENCES device (id);
ALTER TABLE device_command ADD CONSTRAINT device_user_fk FOREIGN KEY (user_id) REFERENCES "user" (id);

CREATE TABLE device_equipment (
    id BIGSERIAL NOT NULL,
    code VARCHAR(128) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    parameters TEXT NULL,
    device_id BIGINT NOT NULL
);

ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_pk PRIMARY KEY (id);
ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_device_fk FOREIGN KEY (device_id) REFERENCES device (id);

CREATE TABLE device_notification (
    id BIGSERIAL NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    notification VARCHAR(128) NOT NULL,
    parameters TEXT NULL,
    device_id BIGINT NOT NULL
);

ALTER TABLE device_notification ADD CONSTRAINT device_notification_pk PRIMARY KEY (id);
ALTER TABLE device_notification ADD CONSTRAINT device_notification_device_fk FOREIGN KEY (device_id) REFERENCES device (id);

CREATE TABLE equipment (
    id BIGSERIAL NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(128) NOT NULL,
    device_class_id BIGINT NOT NULL,
    type VARCHAR(128) NOT NULL,
    data TEXT NULL
);

ALTER TABLE equipment ADD CONSTRAINT equipment_pk PRIMARY KEY (id);
ALTER TABLE equipment ADD CONSTRAINT equipment_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id);
ALTER TABLE equipment ADD CONSTRAINT equipment_code_unique UNIQUE (code);

CREATE TABLE configuration (
  name VARCHAR(32) NOT NULL,
  value VARCHAR(128) NOT NULL
);

ALTER TABLE configuration ADD CONSTRAINT configuration_pk PRIMARY KEY (name);
