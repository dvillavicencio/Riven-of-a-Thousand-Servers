package com.danielvm.destiny2bot.dto.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InteractionResponseData {

  /**
   * Data custom Id (Used only for responding with Modals)
   */
  @JsonProperty("custom_id")
  private String customId;

  /**
   * The message content of the InteractionResponse
   */
  private String content;

  /**
   * List of embeds{@link }
   */
  private List<Embedded> embeds;

  /**
   * List of choices for autocomplete interactions
   */
  private List<Choice> choices;

  /**
   * List of components in the message
   */
  private List<Component> components;

  /**
   * List of partial attachments to send
   */
  private List<Attachment> attachments;

  /**
   * Whether this message should be secret or not (disappears after a time)
   */
  private Integer flags;
}
