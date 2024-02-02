package com.danielvm.destiny2bot.dto.destiny;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerPGCREntry implements Serializable {

  private DestinyUserInfo destinyUserInfo;

  private String characterClass;

  private Integer lightLevel;
}
