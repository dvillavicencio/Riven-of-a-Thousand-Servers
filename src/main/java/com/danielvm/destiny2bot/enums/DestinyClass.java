package com.danielvm.destiny2bot.enums;

import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;

public enum DestinyClass {

  TITAN("Titan", 0),
  HUNTER("Hunter", 1),
  WARLOCK("Warlock", 2);

  @Getter
  private final String name;

  @Getter
  private final Integer code;

  DestinyClass(String name, Integer code) {
    this.name = name;
    this.code = code;
  }

  /**
   * Finds an enum value based on the code
   *
   * @param code The code of the enum value to look for
   * @return the appropriate {@link DestinyClass}
   */
  public static DestinyClass findByCode(Integer code) {
    return Stream.of(DestinyClass.values())
        .filter(e -> Objects.equals(e.getCode(), code))
        .findFirst().orElse(null);
  }
}
