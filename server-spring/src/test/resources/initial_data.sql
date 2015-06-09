-- 1. Default users
-- admin -> test_admin : admin_pass
INSERT INTO "user" (login, password_hash, password_salt, role, status, login_attempts, entity_version) VALUES ('test_admin', '+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=', '9KynX3ShWnFym4y8Dla039py', 0, 0, 0, 0);

INSERT INTO configuration (name, value, entity_version) VALUES ('google.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('google.identity.client.id', 'google_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('facebook.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('facebook.identity.client.id', 'facebook_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('github.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('github.identity.client.id', 'github_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('session.timeout', '1200000', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('allowNetworkAutoCreate', 'true', 0);
