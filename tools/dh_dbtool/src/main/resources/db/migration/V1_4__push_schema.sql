CREATE TABLE user_push_info (
    id BIGSERIAL NOT NULL,
    os_type VARCHAR(32),
    version VARCHAR(32),
    reg_id VARCHAR(64) NOT NULL,
    status INT NOT NULL,
    user_id BIGINT NOT NULL,
    entity_version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE user_push_info ADD CONSTRAINT user_push_info_pk PRIMARY KEY (id);
ALTER TABLE user_push_info ADD CONSTRAINT user_push_info_fk FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE
CASCADE;
