-- V3: PostgreSQL full-text search over pending donations.
-- Adds a tsvector column maintained by a trigger, a GIN index for fast
-- lookup, and backfills existing rows so search works immediately.

ALTER TABLE donations ADD COLUMN search_text tsvector;

CREATE INDEX idx_donations_search ON donations USING GIN (search_text);

CREATE OR REPLACE FUNCTION donations_search_trigger_fn() RETURNS trigger AS $$
BEGIN
    NEW.search_text := to_tsvector('english',
        coalesce(NEW.food_description, '') || ' ' ||
        coalesce(NEW.pickup_city, '') || ' ' ||
        coalesce(NEW.pickup_state, '') || ' ' ||
        coalesce(NEW.food_type, '') || ' ' ||
        coalesce(NEW.quantity, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_donations_search
    BEFORE INSERT OR UPDATE ON donations
    FOR EACH ROW
    EXECUTE FUNCTION donations_search_trigger_fn();

UPDATE donations
SET search_text = to_tsvector('english',
    coalesce(food_description, '') || ' ' ||
    coalesce(pickup_city, '') || ' ' ||
    coalesce(pickup_state, '') || ' ' ||
    coalesce(food_type, '') || ' ' ||
    coalesce(quantity, ''));
