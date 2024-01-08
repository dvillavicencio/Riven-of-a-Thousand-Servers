package com.danielvm.destiny2bot.dto.discord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Member {

  /**
   * Information about the user that sent the interaction
   */
  private DiscordUser user;
}
