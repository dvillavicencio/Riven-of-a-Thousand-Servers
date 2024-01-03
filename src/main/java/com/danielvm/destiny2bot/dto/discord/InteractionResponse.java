package com.danielvm.destiny2bot.dto.discord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InteractionResponse {

  /**
   * The type of the InteractionResponse
   * <br>
   * {@see {@link com.danielvm.destiny2bot.enums.InteractionResponse }}
   */
  private Integer type;
  /**
   * Data attached to the Interaction Response
   */
  private InteractionResponseData data;

  /**
   * Creates the default response for a PING request
   *
   * @return InteractionResponse with null data and type 1
   */
  public static InteractionResponse PING() {
    return new InteractionResponse(1, null);
  }
}