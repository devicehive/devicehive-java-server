DROP INDEX device_class_name_version_idx;
ALTER TABLE device_class DROP CONSTRAINT device_class_name_version_unique;
ALTER TABLE device_class DROP COLUMN IF EXISTS version;