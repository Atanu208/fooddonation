-- V2: Restore the donations status check constraint.
-- The constraint previously created by Hibernate was dropped during an early
-- cleanup and is missing the EXPIRED state, which silently broke the
-- scheduled expiry job. Recreate it with the full set of allowed states.

ALTER TABLE donations DROP CONSTRAINT IF EXISTS donations_status_check;

ALTER TABLE donations ADD CONSTRAINT donations_status_check CHECK (
    status IN ('PENDING', 'ACCEPTED', 'PICKED_UP', 'DELIVERED', 'COMPLETED', 'EXPIRED', 'CANCELLED')
);
