ALTER TABLE lost_found_claims
    ADD COLUMN claimant_contact_phone VARCHAR(20),
    ADD COLUMN contact_share_consent_at TIMESTAMPTZ;

ALTER TABLE lost_found_claims
    ADD CONSTRAINT chk_lost_found_claims_contact_phone CHECK (
        claimant_contact_phone IS NULL
        OR claimant_contact_phone ~ '^\+[1-9][0-9]{7,14}$'
    );
