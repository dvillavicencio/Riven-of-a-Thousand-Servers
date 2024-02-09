package com.danielvm.destiny2bot.factory.creator;

import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WhyAuthorizeButtonMessageCreator implements MessageComponentSource {

  private static final Integer EPHEMERAL_BYTE = 1000000;

  @Override
  public Mono<InteractionResponse> messageComponentResponse(Interaction interaction) {
    return Mono.just(InteractionResponse.builder()
        .type(4)
        .data(InteractionResponseData.builder()
            .content(
                "Without going into technical details, in order to retrieve interesting and relevant "
                + "information regarding your Destiny 2 characters, e.g., like Raid statistics, "
                + "**through Discord**, we need to link both your Bungie and Discord account together. "
                + "This command allows you to do just that. In the consent screen for both Discord "
                + "and Bungie you will be able to review all the permissions the Riven will have with "
                + "both your accounts. Be sure to read and review them well!")
            .flags(EPHEMERAL_BYTE)
            .build())
        .build());
  }
}
