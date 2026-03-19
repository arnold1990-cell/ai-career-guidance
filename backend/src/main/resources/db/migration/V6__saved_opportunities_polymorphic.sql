ALTER TABLE saved_opportunities
    ADD COLUMN IF NOT EXISTS external_key VARCHAR(160),
    ADD COLUMN IF NOT EXISTS opportunity_type VARCHAR(40);
