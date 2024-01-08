package com.danielvm.destiny2bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinyCharacter {

  /**
   * The characterId
   */
  private String characterId;

  /**
   * The character class of this character, e.g., Titan, Warlock, or Hunter
   */
  private String characterClass;

  /**
   * The light level of this character
   */
  private Integer lightLevel;

  /**
   * The race of the character, e.g., human, exo, or awoken
   */
  private String characterRace;

}
