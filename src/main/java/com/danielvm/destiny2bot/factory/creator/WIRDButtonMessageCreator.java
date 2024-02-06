package com.danielvm.destiny2bot.factory.creator;

import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WIRDButtonMessageCreator implements MessageComponentSource {

  @Override
  public Mono<InteractionResponse> messageComponentResponse(Interaction interaction) {
    return Mono.just(InteractionResponse.builder()
        .type(4)
        .data(InteractionResponseData.builder()
            .content(
                "Riven is currently using Bungie.net's API to retrieve the most relevant information about your current raids and characters."
                + " Take note we only use what you allow us to after you authorized Riven to do so. Therefore, nothing like"
                + " passwords, emails, or any other sensitive information is being used here.")
            .build())
        .build());
  }
}
