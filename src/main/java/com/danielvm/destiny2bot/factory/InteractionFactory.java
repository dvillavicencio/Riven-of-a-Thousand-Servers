package com.danielvm.destiny2bot.factory;

import com.danielvm.destiny2bot.enums.SlashCommand;

/**
 * Interaction based factory. Implementations of this interface will return a response based on a
 * given Slash-command. This means that message components and modal submissions should not
 * implement this interface
 *
 * @param <T> The type of the factory
 */
public interface InteractionFactory<T> {

  /**
   * Return a message creator of type T based on a slash-command
   *
   * @param slashCommand The slash command that is invoked
   * @return Message creator of type T
   */
  T messageCreator(SlashCommand slashCommand);
}
