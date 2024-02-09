package com.danielvm.destiny2bot.model.state;

public interface NotificationState {

  /**
   * When update state is called it will modify the Discord message part with the appropriate
   * Discord emoji to represent the status of the current character
   *
   * @param messagePart The message part to modify
   */
  void updateState(MutableMessagePart messagePart);

  /**
   * This is a utility method that will be used by every state to log the status of each character
   *
   * @param user        The Discord user
   * @param character   The Destiny 2 character
   * @param characterId The ID of the Destiny 2 character
   */
  void logStatus(String user, String character, Long characterId);
}
