package com.danielvm.destiny2bot.dto.discord;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddedImage {

  private String url;

  private Integer height;

  private Integer width;
}
