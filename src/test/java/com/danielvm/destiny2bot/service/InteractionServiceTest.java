package com.danielvm.destiny2bot.service;

import static com.danielvm.destiny2bot.enums.InteractionResponse.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT;
import static com.danielvm.destiny2bot.enums.InteractionResponse.CHANNEL_MESSAGE_WITH_SOURCE;
import static com.danielvm.destiny2bot.factory.WeeklyDungeonMessageCreator.MESSAGE_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.DiscordUser;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionData;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.dto.discord.Member;
import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.factory.UserCharacterMessageCreator;
import com.danielvm.destiny2bot.factory.WeeklyDungeonMessageCreator;
import com.danielvm.destiny2bot.util.MessageUtil;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.FirstStep;

@ExtendWith(MockitoExtension.class)
public class InteractionServiceTest {

  @Mock
  AuthorizedMessageRegistry authorizedMessageRegistry;

  @Mock
  UserCharacterMessageCreator userCharacterMessageCreator;

  @Mock
  WeeklyDungeonMessageCreator weeklyDungeonMessageCreator;

  @Mock
  MessageRegistry messageRegistry;

  @InjectMocks
  private InteractionService interactionService;

  @Test
  @DisplayName("Create response is successful for PING interaction request")
  public void handleInteractionForPingRequest() {
    // given: interaction data
    Interaction interaction = new Interaction(
        null, "myApplicationId", 1, null, null);

    // when: the interaction is received
    FirstStep<InteractionResponse> response = StepVerifier.create(
        interactionService.handleInteraction(interaction));

    // then: the response received is correct
    response
        .assertNext(result -> assertThat(result.getType()).isEqualTo(1))
        .verifyComplete();
  }

  @Test
  @DisplayName("Create response is successful for APPLICATION_COMMAND interaction request that doesn't require authorization")
  public void handleInteractionFor() {
    // given: interaction data from an application command (slash command)
    Interaction interaction = new Interaction(
        null, "myApplicationId", 2,
        new InteractionData("someId", "weekly_dungeon", 1),
        new Member(new DiscordUser("someId", "myUsername")));

    String endDate = MessageUtil.formatDate(LocalDate.now());
    String dungeon = "Duality";
    String content = MESSAGE_TEMPLATE.formatted(dungeon, endDate);

    InteractionResponse message = InteractionResponse.builder()
        .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(InteractionResponseData.builder()
            .content(content)
            .build())
        .build();

    when(messageRegistry.messageCreator(CommandEnum.WEEKLY_DUNGEON))
        .thenReturn(weeklyDungeonMessageCreator);

    when(weeklyDungeonMessageCreator.createResponse())
        .thenReturn(Mono.just(message));

    // when: the interaction is received
    FirstStep<InteractionResponse> response = StepVerifier.create(
        interactionService.handleInteraction(interaction));

    // then: the response received is correct and the message returned has the correct content
    response
        .assertNext(result -> {
          assertThat(result.getType()).isEqualTo(CHANNEL_MESSAGE_WITH_SOURCE.getType());
          assertThat(result.getData().getContent()).isEqualTo(content);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Create response is successful for APPLICATION_COMMAND_AUTOCOMPLETE interaction request that requires authentication")
  public void handleInteractionForAuthorizedAutocomplete() {
    // given: interaction data from a slash-command autocomplete
    String userId = "userId";
    Interaction interaction = new Interaction(
        null, "myApplicationId", 4,
        new InteractionData("someId", "raid_stats", 1),
        new Member(new DiscordUser(userId, "myUsername")));

    List<Choice> choices = List.of(
        new Choice("Character 1", "1"),
        new Choice("Character 1", "1"),
        new Choice("Character 1", "1"),
        new Choice("All", "All")
    );
    InteractionResponse message = InteractionResponse.builder()
        .type(APPLICATION_COMMAND_AUTOCOMPLETE_RESULT.getType())
        .data(InteractionResponseData.builder()
            .choices(choices)
            .build())
        .build();

    when(authorizedMessageRegistry.messageCreator(CommandEnum.RAID_STATS))
        .thenReturn(userCharacterMessageCreator);

    when(userCharacterMessageCreator.createResponse(userId))
        .thenReturn(Mono.just(message));

    // when: the interaction is received
    FirstStep<InteractionResponse> response = StepVerifier.create(
        interactionService.handleInteraction(interaction));

    // then: the response received is correct and the message returned has the correct content
    response
        .assertNext(result -> {
          assertThat(result.getType()).isEqualTo(APPLICATION_COMMAND_AUTOCOMPLETE_RESULT.getType());
          assertThat(result.getData().getChoices()).containsAll(choices);
        })
        .verifyComplete();
  }
}
