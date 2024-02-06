package com.danielvm.destiny2bot.dto.discord;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DmMessageResponse {

  /**
   * ID of the message sent
   */
  private Long id;

  /**
   * Type of the message sent
   */
  private Integer type;

  /**
   * Content of the message sent to the user
   */
  private String content;

  /**
   * The channelId between the discord bot and the user
   */
  @JsonAlias("channel_id")
  private Long channelId;

  /**
   * List of embeds that were sent with the DM message
   */
  private List<Embedded> embeds;
}
