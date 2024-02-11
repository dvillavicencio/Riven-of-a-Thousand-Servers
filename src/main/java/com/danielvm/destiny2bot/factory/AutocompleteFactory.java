package com.danielvm.destiny2bot.factory;

import com.danielvm.destiny2bot.enums.SlashCommand;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.creator.AutocompleteSource;
import com.danielvm.destiny2bot.factory.creator.ExperimentalRaidStatsCreator;
import com.danielvm.destiny2bot.factory.creator.RaidMapMessageCreator;
import com.danielvm.destiny2bot.factory.creator.RaidStatsMessageCreator;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AutocompleteFactory implements InteractionFactory<AutocompleteSource> {

  private final Map<SlashCommand, AutocompleteSource> autocompleteFactory;

  public AutocompleteFactory(
      RaidStatsMessageCreator raidStatsMessageCreator,
      RaidMapMessageCreator raidMapMessageCreator,
      ExperimentalRaidStatsCreator experimentalRaidStatsCreator) {
    this.autocompleteFactory = Map.of(
        SlashCommand.RAID_STATS, raidStatsMessageCreator,
        SlashCommand.RAID_MAP, raidMapMessageCreator,
        SlashCommand.EXPERIMENTAL_RAID_STATS, experimentalRaidStatsCreator);
  }

  @Override
  public AutocompleteSource messageCreator(SlashCommand command) {
    AutocompleteSource creator = autocompleteFactory.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }
}
