CREATE TABLE user_page_information
(
    user_discord_id BIGINT PRIMARY KEY,
    number_of_pages INTEGER,
    last_page       INTEGER,
    CONSTRAINT discord_id_fk FOREIGN KEY (user_discord_id) REFERENCES bot_user (discord_id)
);