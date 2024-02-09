package com.danielvm.destiny2bot.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.discord.Component;
import com.danielvm.destiny2bot.dto.discord.DiscordUser;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.Member;
import com.danielvm.destiny2bot.enums.InteractionResponseType;
import com.danielvm.destiny2bot.factory.creator.AuthorizeMessageCreator;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.FirstStep;

@ExtendWith(MockitoExtension.class)
public class AuthorizeMessageCreatorTest {

  List<String> scopes = List.of("someScope");
  String authUrl = "https://some.authorization.url/oauth2/authorize";
  String clientId = "someClientId";
  String callbackUrl = "https://some.callback.url/discord/callback";

  @Mock
  DiscordConfiguration discordConfiguration;

  @Mock
  BotUserRepository userRepository;

  @InjectMocks
  AuthorizeMessageCreator sut;

  @Test
  @DisplayName("Create message is successful for an un-authorized user")
  public void createMessageIsSuccessful() {
    Component authorizeButton = Component.builder()
        .type(2)
        .style(5)
        .url(OAuth2Util.discordAuthorizationUrl(authUrl, clientId, callbackUrl, scopes.get(0)))
        .label("Authorize")
        .build();
    Component whyButton = Component.builder()
        .customId("why_authorize_button")
        .label("Why?")
        .type(2)
        .style(1)
        .build();

    Interaction interaction = Interaction.builder()
        .member(new Member(new DiscordUser("1", "Deahtstroke")))
        .build();
    when(userRepository.existsById(1L)).thenReturn(false);
    when(discordConfiguration.getAuthorizationUrl()).thenReturn(authUrl);
    when(discordConfiguration.getClientId()).thenReturn(clientId);
    when(discordConfiguration.getCallbackUrl()).thenReturn(callbackUrl);
    when(discordConfiguration.getScopes()).thenReturn(scopes);

    // when: create message is called
    FirstStep<com.danielvm.destiny2bot.dto.discord.InteractionResponse> response = StepVerifier.create(
        sut.createResponse(interaction));

    // then: the message created is correct
    response.assertNext(interactionResponse -> {
      var embedded = interactionResponse.getData().getEmbeds().get(0);
      var components = interactionResponse.getData().getComponents().get(0)
          .getComponents(); // action row 1
      var whyComponent = components.stream()
          .filter(c -> Objects.equals(c.getCustomId(), "why_authorize_button")).findFirst()
          .orElse(null);
      var authorizeComponent = components.stream()
          .filter(c -> Objects.equals(c.getLabel(), "Authorize"))
          .findFirst().orElse(null);

      assertThat(interactionResponse.getType()).isEqualTo(
          InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getType());

      assertThat(embedded.getDescription()).isEqualTo(
          AuthorizeMessageCreator.MESSAGE_DESCRIPTION);
      assertThat(embedded.getTitle()).isEqualTo(AuthorizeMessageCreator.MESSAGE_TITLE);

      assertThat(whyComponent).isNotNull();
      assertThat(whyComponent).isEqualTo(whyButton);

      assertThat(authorizeComponent).isNotNull();
      assertThat(authorizeComponent).isEqualTo(authorizeButton);
    }).verifyComplete();
  }

  @Test
  @DisplayName("Create message is successful for an authorized user")
  public void createMessageForAuthorizedUser() {
    Interaction interaction = Interaction.builder()
        .member(new Member(new DiscordUser("1", "Deahtstroke")))
        .build();
    when(userRepository.existsById(1L)).thenReturn(true);
    when(discordConfiguration.getAuthorizationUrl()).thenReturn(authUrl);
    when(discordConfiguration.getClientId()).thenReturn(clientId);
    when(discordConfiguration.getCallbackUrl()).thenReturn(callbackUrl);
    when(discordConfiguration.getScopes()).thenReturn(scopes);

    // when: create message is called
    FirstStep<com.danielvm.destiny2bot.dto.discord.InteractionResponse> response = StepVerifier.create(
        sut.createResponse(interaction));

    // then: the message created is correct
    response.assertNext(interactionResponse -> {
      Assertions.assertThat(interactionResponse.getData().getContent())
          .isEqualTo("You have already authorized the bot! Don't worry, we gotchu covered");
    }).verifyComplete();
  }
}
