package com.danielvm.destiny2bot.factory;

import com.danielvm.destiny2bot.enums.SlashCommand;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * This service has a single factory map that contains all the entries between slash-commands and
 * their corresponding message creation services.
 */
@Component
public class MessageFactory {

  private final Map<SlashCommand, MessageSourceCreator> messageFactory;

  public MessageFactory(
      RaidDiagramMessageCreator raidDiagramMessageCreator,
      WeeklyRaidMessageCreator weeklyRaidMessageCreator,
      WeeklyDungeonMessageCreator weeklyDungeonMessageCreator,
      AuthorizeMessageCreator authorizeMessageCreator) {
    this.messageFactory = Map.of(
        SlashCommand.WEEKLY_RAID, weeklyRaidMessageCreator,
        SlashCommand.WEEKLY_DUNGEON, weeklyDungeonMessageCreator,
        SlashCommand.AUTHORIZE, authorizeMessageCreator,
        SlashCommand.RAID_MAP, raidDiagramMessageCreator);
  }

  /**
   * Return the corresponding message-creator associated with a slash-command
   *
   * @param command The {@link SlashCommand} to get the factory for
   * @return an implementation of {@link MessageSourceCreator}
   * @throws ResourceNotFoundException If no creator is found for the given command
   */
  public MessageSourceCreator messageCreator(SlashCommand command) {
    MessageSourceCreator creator = messageFactory.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }
}
