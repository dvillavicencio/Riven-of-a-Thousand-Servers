package com.danielvm.destiny2bot.factory;

import com.danielvm.destiny2bot.enums.SlashCommand;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.handler.ApplicationCommandSource;
import com.danielvm.destiny2bot.factory.handler.AuthorizeHandler;
import com.danielvm.destiny2bot.factory.handler.RaidStatsHandler;
import com.danielvm.destiny2bot.factory.handler.RaidMapHandler;
import com.danielvm.destiny2bot.factory.handler.WeeklyDungeonHandler;
import com.danielvm.destiny2bot.factory.handler.WeeklyRaidHandler;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * This service has a single factory map that contains all the entries between slash-commands and
 * their corresponding message creation services.
 */
@Component
public class ApplicationCommandFactory implements SlashCommandHandler<ApplicationCommandSource> {

  private final Map<SlashCommand, ApplicationCommandSource> messageFactory;

  public ApplicationCommandFactory(
      RaidMapHandler raidMapHandler,
      WeeklyRaidHandler weeklyRaidHandler,
      WeeklyDungeonHandler weeklyDungeonHandler,
      AuthorizeHandler authorizeMessageHandler,
      RaidStatsHandler raidStatsHandler) {
    this.messageFactory = Map.of(
        SlashCommand.WEEKLY_RAID, weeklyRaidHandler,
        SlashCommand.WEEKLY_DUNGEON, weeklyDungeonHandler,
        SlashCommand.AUTHORIZE, authorizeMessageHandler,
        SlashCommand.RAID_MAP, raidMapHandler,
        SlashCommand.RAID_STATS, raidStatsHandler);
  }

  @Override
  public ApplicationCommandSource messageCreator(SlashCommand command) {
    ApplicationCommandSource creator = messageFactory.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }
}
