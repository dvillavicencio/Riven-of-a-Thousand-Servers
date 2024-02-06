package com.danielvm.destiny2bot.dto.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenDmChannelRequest {

  /**
   * The Id of the Discord user to open a DM for
   */
  @JsonProperty("recipient_id")
  private Long recipientId;
}
