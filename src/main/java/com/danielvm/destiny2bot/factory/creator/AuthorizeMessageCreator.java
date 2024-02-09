package com.danielvm.destiny2bot.factory.creator;

import static com.danielvm.destiny2bot.enums.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE;

import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.discord.Component;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.List;
import reactor.core.publisher.Mono;

@org.springframework.stereotype.Component
public class AuthorizeMessageCreator implements ApplicationCommandSource {

  public static final String MESSAGE_TITLE = "**Link Bungie and Discord accounts here**";
  public static final String MESSAGE_DESCRIPTION = """
      Riven can fulfill commands that are unique to your Destiny 2 characters.
      However, in order for her to do that we must first link your Discord account with your Destiny 2 account.
      """;
  private static final Integer EPHEMERAL_BYTE = 1000000;

  private final DiscordConfiguration discordConfiguration;
  private final BotUserRepository userRepository;

  public AuthorizeMessageCreator(
      DiscordConfiguration discordConfiguration,
      BotUserRepository userRepository) {
    this.discordConfiguration = discordConfiguration;
    this.userRepository = userRepository;
  }

  @Override
  public Mono<InteractionResponse> createResponse(Interaction interaction) {
    String authUrl = discordConfiguration.getAuthorizationUrl();
    String clientId = discordConfiguration.getClientId();
    String callbackUrl = discordConfiguration.getCallbackUrl();
    String scopes = String.join(",", discordConfiguration.getScopes());

    String authorizationUrl = OAuth2Util.discordAuthorizationUrl(authUrl, clientId,
        callbackUrl, scopes);

    Embedded accountLinkEmbed = Embedded.builder()
        .title(MESSAGE_TITLE)
        .description(MESSAGE_DESCRIPTION)
        .build();

    List<Component> buttons = List.of(
        Component.builder().type(1)
            .components(List.of(
                Component.builder() // 'Authorize' link button
                    .type(2)
                    .style(5)
                    .url(authorizationUrl)
                    .label("Authorize")
                    .build(),
                Component.builder() // 'Why?' button
                    .customId("why_authorize_button")
                    .label("Why?")
                    .type(2)
                    .style(1)
                    .build()))
            .build());

    return Mono.just(
            userRepository.existsById(Long.valueOf(interaction.getMember().getUser().getId())))
        .map(exists -> {
          if (exists) {
            return InteractionResponse.builder()
                .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
                .data(InteractionResponseData.builder()
                    .content("You have already authorized the bot! Don't worry, we gotchu covered")
                    .flags(EPHEMERAL_BYTE)
                    .build())
                .build();
          }
          return InteractionResponse.builder()
              .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
              .data(InteractionResponseData.builder()
                  .embeds(List.of(accountLinkEmbed))
                  .flags(EPHEMERAL_BYTE)
                  .components(buttons)
                  .build())
              .build();
        });
  }
}
