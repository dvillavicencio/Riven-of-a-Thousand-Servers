CREATE TABLE IF NOT EXISTS bot_user
(
    discord_id              BIGINT PRIMARY KEY,
    discord_username        VARCHAR(50) NOT NULL,
    bungie_membership_id    BIGINT      NOT NULL,
    bungie_membership_type  INTEGER     NOT NULL,
    bungie_access_token     TEXT        NOT NULL,
    bungie_refresh_token    TEXT        NOT NULL,
    bungie_token_expiration BIGINT      NOT NULL
);

CREATE TABLE IF NOT EXISTS bungie_user_character
(
    character_id    BIGINT PRIMARY KEY,
    light_level     INTEGER     NOT NULL,
    destiny_class   VARCHAR(10) NOT NULL,
    discord_user_id BIGINT,
    CONSTRAINT user_fk FOREIGN KEY (discord_user_id) REFERENCES bot_user (discord_id)
);

CREATE TABLE IF NOT EXISTS character_raid
(
    instance_id          BIGINT PRIMARY KEY,
    raid_start_timestamp TIMESTAMP    NOT NULL,
    is_from_beginning    BOOLEAN      NOT NULL,
    completed            BOOLEAN      NOT NULL,
    raid_name            VARCHAR(100) NOT NULL,
    number_of_deaths     INTEGER      NOT NULL,
    oponents_defeated    INTEGER      NOT NULL,
    kill_death_assists   DECIMAL      NOT NULL,
    raid_duration        VARCHAR(50)  NOT NULL,
    user_character_id    BIGINT,
    CONSTRAINT user_character_fk FOREIGN KEY (user_character_id) REFERENCES bungie_user_character (character_id)
);

CREATE TABLE IF NOT EXISTS raid_participant
(
    membership_id   BIGINT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    character_class VARCHAR(10)  NOT NULL,
    icon_path       VARCHAR(100) NOT NULL,
    completed       BOOLEAN      NOT NULL,
    raid_instance   BIGINT,
    FOREIGN KEY (raid_instance) REFERENCES character_raid (instance_id)
);
