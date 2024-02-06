package com.danielvm.destiny2bot.model.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MutableMessagePart {

  /**
   * The current state of the indexing content denoted by an emoji
   */
  private String emoji;

  /**
   * The current object that's being indexed or in the process of changing its state
   */
  private String statefulContent;

}
