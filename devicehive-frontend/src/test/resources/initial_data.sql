-- 1. Default users
-- admin -> test_admin : admin_pass
INSERT INTO "user" (login, password_hash, password_salt, role, status, login_attempts) VALUES ('test_admin', '+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=', '9KynX3ShWnFym4y8Dla039py', 0, 0, 0);

INSERT INTO access_key (label, key, expiration_date, user_id, entity_version) VALUES ('Access Key for dhadmin', '1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=', null, 1, 1);
INSERT INTO access_key_permission (access_key_id, entity_version) VALUES (1, 1);

INSERT INTO configuration (name, value, entity_version) VALUES ('google.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('google.identity.client.id', 'google_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('facebook.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('facebook.identity.client.id', 'facebook_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('github.identity.allowed', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('github.identity.client.id', 'github_id', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('session.timeout', '1200000', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('allowNetworkAutoCreate', 'true', 0);
INSERT INTO configuration (name, value, entity_version) VALUES ('user.login.lastTimeout', '1000', 0);

-- 2. Default device classes
INSERT INTO device_class (name, is_permanent, offline_timeout) VALUES ('Sample VirtualLed Device', FALSE, 600);

-- 3. Default networks
INSERT INTO network (name, description) VALUES ('VirtualLed Sample Network', 'A DeviceHive network for VirtualLed sample');

-- 4. Default devices
INSERT INTO device (guid, name, status, network_id, device_class_id) VALUES ('E50D6085-2ABA-48E9-B1C3-73C673E414BE', 'Sample VirtualLed Device', 'Offline', 1, 1);