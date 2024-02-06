package com.danielvm.destiny2bot.dto.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenDmChannelResponse {

  /**
   * Id of the message channel to speak through a DM
   */
  private Long id;

  /**
   * Type of the message channel opened
   */
  private Integer type;

  /**
   * Id of the last message
   */
  @JsonProperty(value = "last_message_id")
  private Long lastMessageId;

  /**
   * Bitwise flags
   */
  private Integer flags;

  /**
   * The list of Discord recipients (ideally just one for a DM)
   */
  private List<DiscordUser> recipients;

}
