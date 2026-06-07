-- Add user_type column (PASSENGER or ADMIN — for event routing)
ALTER TABLE quarkus.users ADD COLUMN user_type VARCHAR(50);

-- Backfill: derive user_type from existing roles column
-- If roles contains 'PASSENGER' → user_type = 'PASSENGER'
-- If roles contains 'ADMIN' or 'editor' or 'admin' or 'super-admin' → user_type = 'ADMIN'
-- Otherwise default to 'PASSENGER'
UPDATE quarkus.users SET user_type =
  CASE
    WHEN roles ILIKE '%PASSENGER%' THEN 'PASSENGER'
    WHEN roles ILIKE '%ADMIN%' OR roles ILIKE '%editor%' OR roles ILIKE '%super-admin%' THEN 'ADMIN'
    ELSE 'PASSENGER'
  END
WHERE user_type IS NULL;

-- Make user_type NOT NULL
ALTER TABLE quarkus.users ALTER COLUMN user_type SET NOT NULL;

-- Note: roles column stays as-is but now only stores Keycloak composite roles (basic, standard, premium, editor, admin, super-admin)
-- We may want to clean up roles that have PASSENGER/ADMIN mixed in, but that's a separate concern
