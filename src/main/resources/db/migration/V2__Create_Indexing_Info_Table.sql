CREATE TABLE user_indexing_information
(
    user_discord_id BIGINT PRIMARY KEY,
    number_of_pages INTEGER DEFAULT 1,
    last_page       INTEGER NOT NULL,
    is_indexing     BOOLEAN NOT NULL,
    CONSTRAINT discord_id_fk FOREIGN KEY (user_discord_id) REFERENCES bot_user (discord_id)
);