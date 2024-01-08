package com.danielvm.destiny2bot.enums;

import java.util.Arrays;
import java.util.Objects;
import lombok.Getter;

public enum DestinyRace {

  HUMAN("Human", 0),
  AWOKEN("Awoken", 1),
  EXO("Exo", 2);

  @Getter
  private final String name;
  @Getter
  private final Integer code;

  DestinyRace(String name, Integer code) {
    this.code = code;
    this.name = name;
  }

  /**
   * Finds an enum value based on the code
   *
   * @param code The code of the enum value to look for
   * @return the appropriate {@link DestinyRace}
   */
  public static DestinyRace findByCode(Integer code) {
    return Arrays.stream(DestinyRace.values())
        .filter(e -> Objects.equals(e.code, code))
        .findFirst().orElse(null);
  }
}
