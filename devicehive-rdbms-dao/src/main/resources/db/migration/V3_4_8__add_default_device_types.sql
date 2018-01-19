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

-- Add default list of device types
INSERT INTO device_type (name, description) VALUES
  ('Cameras', 'Enable your smart home cameras to enable real time streaming');
INSERT INTO device_type (name, description) VALUES
  ('Lightning', 'Devices to turn smart lights on and off, change the brightness, specify colors');
INSERT INTO device_type (name, description) VALUES
  ('Door Locks', 'Check the state of your door locks and manipulate if needed');
INSERT INTO device_type (name, description) VALUES
  ('Thermostats', 'Devices to control thermostat settings and to report current temperatures');
INSERT INTO device_type (name, description) VALUES
  ('Entertainment', 'Control of your smart TVs, receivers, and other entertainment devices');
INSERT INTO device_type (name, description) VALUES
  ('Cars', 'Devices to develop your own smart cars');
INSERT INTO device_type (name, description) VALUES
  ('Cooking', 'Cooking devices for hands-free control in your kitchen');