package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.AuthorizeMessageCreator;
import com.danielvm.destiny2bot.factory.MessageResponseFactory;
import com.danielvm.destiny2bot.factory.WeeklyDungeonMessageCreator;
import com.danielvm.destiny2bot.factory.WeeklyRaidMessageCreator;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * This service has a single factory map that contains all the entries between slash-commands and
 * their corresponding message creation services.
 */
@Component
public class MessageRegistry {

  private final Map<CommandEnum, MessageResponseFactory> messageFactory;

  public MessageRegistry(
      WeeklyRaidMessageCreator weeklyRaidMessageCreator,
      WeeklyDungeonMessageCreator weeklyDungeonMessageCreator,
      AuthorizeMessageCreator authorizeMessageCreator) {
    this.messageFactory = Map.of(
        CommandEnum.WEEKLY_RAID, weeklyRaidMessageCreator,
        CommandEnum.WEEKLY_DUNGEON, weeklyDungeonMessageCreator,
        CommandEnum.AUTHORIZE, authorizeMessageCreator);
  }

  /**
   * Return the corresponding message-creator associated with a Command in {@link CommandEnum}
   *
   * @param command The command to get the factory for
   * @return an implementation of {@link MessageResponseFactory}
   * @throws ResourceNotFoundException If no creator is found for the given command
   */
  public MessageResponseFactory messageCreator(CommandEnum command) {
    MessageResponseFactory creator = messageFactory.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }
}
