package com.danielvm.destiny2bot.dto.destiny.characters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterInfo {

  private Long membershipId;
  private Integer membershipType;
  private Long CharacterId;
  private Integer light;
  private Integer raceType;
  private Integer genderType;
  private Integer classType;

}
