-- Reference data for initial database setup:

-- 1. Default users
INSERT INTO "user"
  (login, password_hash, password_salt, role, status, login_attempts)
VALUES
  ('admin', 'T6v58XVyDZ/pqmSbSrf5b7bmNIT6TJQt6myUGCcm4as=', '0QSyWc39YneN2CbYrsCg88oK', 0, 0, 0);

-- 2. Default device classes
INSERT INTO device_class
  (name, version, is_permanent, offline_timeout)
VALUES
  ('Sample VirtualLed Device', 1.0, false, 600);

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