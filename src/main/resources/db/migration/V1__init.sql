SET
search_path = game_schema;

-- functions:
--
CREATE OR REPLACE FUNCTION prevent_update_and_delete_fn()
RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Modifying or deleting from this table is forbidden';
END;
$$ LANGUAGE plpgsql;

--
CREATE OR REPLACE FUNCTION updated_at_prevent_update_fn()
RETURNS trigger AS $$
BEGIN
  IF NEW.updated_at IS DISTINCT FROM OLD.updated_at THEN
    RAISE EXCEPTION 'Cannot modify column ''updated_at'' on this table';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- tables:
--
CREATE TABLE game_sessions (
                               game_session_id UUID
                                   PRIMARY KEY DEFAULT uuid_generate_v1(),
                               dealer_score INT NOT NULL
                                   CONSTRAINT ch_min_dealer_score CHECK (dealer_score >= 0)
                                               DEFAULT 0,
                               status VARCHAR(16) NOT NULL
                                   CONSTRAINT ch_game_session_status CHECK (status IN ('FINISHED', 'CANCELED')),
                               started_at TIMESTAMPTZ NOT NULL
                                               DEFAULT now(),
                               finished_at TIMESTAMPTZ NOT NULL
                                   CONSTRAINT finished_after_started CHECK (finished_at >= started_at)
);

--
CREATE TABLE game_session_seats  (
                                     game_session_seat_id UUID PRIMARY KEY
                                         DEFAULT uuid_generate_v1(),
                                     game_session_id UUID NOT NULL
                                         CONSTRAINT fk_game_session_id
                                             REFERENCES game_sessions (game_session_id)
                                             ON DELETE CASCADE,
                                     seat_number INT NOT NULL
                                         CONSTRAINT chk_seat_number_valid
                                             CHECK (seat_number BETWEEN 1 AND 7),
                                     user_id UUID NOT NULL,
                                     round_result VARCHAR(16) NOT NULL
                                         CHECK (round_result IN ('BLACKJACK', 'WIN', 'PUSH', 'CASH_OUT', 'LOSE')),
                                     bet NUMERIC(17,2) NOT NULL,

                                     CONSTRAINT unique_seat_in_game UNIQUE (game_session_id, seat_number)
);


-- triggers:
--
CREATE TRIGGER game_session_seats_protect_from_update_and_delete_trg
    BEFORE UPDATE OR DELETE ON game_session_seats
FOR EACH ROW
EXECUTE FUNCTION prevent_update_and_delete_fn();