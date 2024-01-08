package com.danielvm.destiny2bot.factory;

import static com.danielvm.destiny2bot.enums.InteractionResponse.CHANNEL_MESSAGE_WITH_SOURCE;

import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeMessageCreator implements MessageResponseFactory {

  public static final String MESSAGE_TITLE = "Link Bungie and Discord accounts here";
  public static final String MESSAGE_DESCRIPTION = """
      Riven can grant you wishes unique to your Bungie account.
      However, you need to link your Discord and Bungie account for that to happen.
      This slash comma-
      I mean, this _wish_, allows her to do that.
      """;
  private final DiscordConfiguration discordConfiguration;

  public AuthorizeMessageCreator(DiscordConfiguration discordConfiguration) {
    this.discordConfiguration = discordConfiguration;
  }

  @Override
  public Mono<InteractionResponse> createResponse() {
    String authUrl = discordConfiguration.getAuthorizationUrl();
    String clientId = discordConfiguration.getClientId();
    String callbackUrl = discordConfiguration.getCallbackUrl();
    String scopes = String.join(",", discordConfiguration.getScopes());

    Embedded accountLinkEmbed = Embedded.builder()
        .title(MESSAGE_TITLE)
        .description(MESSAGE_DESCRIPTION)
        .url(OAuth2Util.discordAuthorizationUrl(authUrl, clientId, callbackUrl, scopes))
        .build();

    return Mono.just(InteractionResponse.builder()
        .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(InteractionResponseData.builder()
            .embeds(List.of(accountLinkEmbed))
            .build()
        ).build());
  }
}
