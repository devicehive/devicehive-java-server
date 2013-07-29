-- add cascade delete to constraints
ALTER TABLE device DROP CONSTRAINT device_network_fk;
ALTER TABLE device DROP CONSTRAINT device_device_class_fk;
ALTER TABLE device_command DROP CONSTRAINT device_command_device_fk;
ALTER TABLE device_equipment DROP CONSTRAINT device_equipment_device_fk;
ALTER TABLE device_notification DROP CONSTRAINT device_notification_device_fk;
ALTER TABLE equipment DROP CONSTRAINT equipment_device_class_fk;


ALTER TABLE device ADD CONSTRAINT device_network_fk FOREIGN KEY (network_id) REFERENCES network (id)  ON DELETE CASCADE;
ALTER TABLE device ADD CONSTRAINT device_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id)  ON DELETE CASCADE;
ALTER TABLE device_command ADD CONSTRAINT device_command_device_fk FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE;
ALTER TABLE device_equipment ADD CONSTRAINT device_equipment_device_fk FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE;
ALTER TABLE device_notification ADD CONSTRAINT device_notification_device_fk FOREIGN KEY (device_id) REFERENCES device (id)  ON DELETE CASCADE;
ALTER TABLE equipment ADD CONSTRAINT equipment_device_class_fk FOREIGN KEY (device_class_id) REFERENCES device_class (id)  ON DELETE CASCADE;
