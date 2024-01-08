package com.danielvm.destiny2bot.factory;

import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.dto.WeeklyActivity;
import com.danielvm.destiny2bot.enums.ActivityMode;
import com.danielvm.destiny2bot.enums.InteractionResponse;
import com.danielvm.destiny2bot.service.WeeklyActivitiesService;
import com.danielvm.destiny2bot.util.MessageUtil;
import java.time.ZonedDateTime;
import org.assertj.core.api.Assertions;
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
public class WeeklyDungeonMessageCreatorTest {

  @Mock
  WeeklyActivitiesService weeklyActivitiesService;

  @InjectMocks
  WeeklyDungeonMessageCreator sut;

  @Test
  @DisplayName("Create message is successful")
  public void createMessageIsSuccessful() {
    WeeklyActivity weeklyActivity = new WeeklyActivity(
        "dungeon", "description", ZonedDateTime.now(), ZonedDateTime.now());
    when(weeklyActivitiesService.getWeeklyActivity(ActivityMode.DUNGEON))
        .thenReturn(Mono.just(weeklyActivity));

    // when: create message is called
    FirstStep<com.danielvm.destiny2bot.dto.discord.InteractionResponse> response = StepVerifier.create(sut.createResponse());

    // then: the message created is correct
    String expectedMessage = WeeklyDungeonMessageCreator.MESSAGE_TEMPLATE.formatted(
        weeklyActivity.getName(),
        MessageUtil.formatDate(weeklyActivity.getEndDate().toLocalDate()));
    response
        .assertNext(ir -> {
          Assertions.assertThat(ir.getType())
              .isEqualTo(InteractionResponse.CHANNEL_MESSAGE_WITH_SOURCE.getType());
          Assertions.assertThat(ir.getData().getContent())
              .isEqualTo(expectedMessage);
        }).verifyComplete();
  }
}
