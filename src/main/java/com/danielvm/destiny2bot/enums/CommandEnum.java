package com.danielvm.destiny2bot.enums;

import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import java.util.stream.Stream;
import lombok.Getter;

public enum CommandEnum {

  AUTHORIZE("authorize", false),
  WEEKLY_DUNGEON("weekly_dungeon", false),
  WEEKLY_RAID("weekly_raid", false),
  RAID_STATS("raid_stats", true);

  @Getter
  private final String commandName;

  @Getter
  private final boolean authorized;

  CommandEnum(String commandName, boolean authorized) {
    this.commandName = commandName;
    this.authorized = authorized;
  }

  /**
   * Find a value of this enum by the Command name
   *
   * @param commandName The name of this command
   * @return {@link CommandEnum}
   */
  public static CommandEnum findByName(String commandName) {
    return Stream.of(CommandEnum.values())
        .filter(e -> e.getCommandName().equalsIgnoreCase(commandName))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(
            "Command with name [%s] was not found".formatted(commandName)));
  }

}
