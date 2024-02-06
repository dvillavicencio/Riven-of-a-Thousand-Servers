package com.danielvm.destiny2bot.dto.discord;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DmMessageRequest {

  /**
   * Content to send through a discord message
   */
  @Length(max = 2000)
  private String content;

  /**
   * List of embedded elements
   */
  private List<Embedded> embeds;

  /**
   * List of component elements
   */
  private List<Component> components;

}
