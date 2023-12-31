package com.danielvm.destiny2bot.dto.discord;

import lombok.Data;

@Data
public class Member {

  /**
   * Information about the user that sent the interaction
   */
  private DiscordUser user;
}
