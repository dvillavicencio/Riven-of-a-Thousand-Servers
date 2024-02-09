CREATE TABLE user_indexing_information
(
    user_discord_id BIGINT PRIMARY KEY,
    is_indexing     BOOLEAN,
    constraint discord_id_fk FOREIGN KEY (user_discord_id) REFERENCES bot_user (discord_id)
);

CREATE TABLE user_character_indexing_information
(
    character_id BIGINT PRIMARY KEY,
    bot_user_id  BIGINT,
    last_page    INTEGER,
    CONSTRAINT character_fk FOREIGN KEY (character_id) REFERENCES bungie_user_character (character_id),
    CONSTRAINT bot_user_id_fk FOREIGN KEY (bot_user_id) REFERENCES user_indexing_information (user_discord_id)
);