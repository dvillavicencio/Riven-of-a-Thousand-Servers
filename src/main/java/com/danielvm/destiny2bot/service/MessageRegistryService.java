package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.AuthorizeAccountMessage;
import com.danielvm.destiny2bot.factory.MessageResponseFactory;
import com.danielvm.destiny2bot.factory.WeeklyDungeonMessage;
import com.danielvm.destiny2bot.factory.WeeklyRaidMessage;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * This service has a single factory map that contains all the entries between slash-commands and
 * their corresponding message creation services.
 */
@Component
public class MessageRegistryService {

  private final Map<CommandEnum, MessageResponseFactory> MESSAGE_FACTORY;

  public MessageRegistryService(
      WeeklyRaidMessage weeklyRaidMessage,
      WeeklyDungeonMessage weeklyDungeonMessage,
      AuthorizeAccountMessage authorizeAccountMessage) {
    this.MESSAGE_FACTORY = Map.of(
        CommandEnum.WEEKLY_RAID, weeklyRaidMessage,
        CommandEnum.WEEKLY_DUNGEON, weeklyDungeonMessage,
        CommandEnum.AUTHORIZE, authorizeAccountMessage);
  }

  /**
   * Return the corresponding message-creator associated with a Command in {@link CommandEnum}
   *
   * @param command The command to get the factory for
   * @return an implementation of {@link MessageResponseFactory}
   * @throws ResourceNotFoundException If no creator is found for the given command
   */
  public MessageResponseFactory getFactory(CommandEnum command) {
    MessageResponseFactory creator = MESSAGE_FACTORY.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }
}
