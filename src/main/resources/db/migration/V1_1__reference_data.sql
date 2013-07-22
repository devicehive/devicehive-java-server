-- Reference data for initial database setup:
-- 1. Default users

INSERT INTO "user"
  (login, password_hash, password_salt, role, status, login_attempts)
VALUES
  ('admin', 'T6v58XVyDZ/pqmSbSrf5b7bmNIT6TJQt6myUGCcm4as=', '0QSyWc39YneN2CbYrsCg88oK', 0, 0, 0)