-- Admin rejection reason, surfaced to the seller in "My Listings" / admin-web.
ALTER TABLE advertisement ADD COLUMN rejection_reason TEXT NULL;
