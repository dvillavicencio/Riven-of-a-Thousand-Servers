package com.danielvm.destiny2bot.dto.discord;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscordUserResponse {

  private Long id;

  private String username;

  private String avatar;

  private String locale;
}
