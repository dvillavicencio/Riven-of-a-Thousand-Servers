package com.danielvm.destiny2bot.factory;

import static com.danielvm.destiny2bot.enums.InteractionResponse.CHANNEL_MESSAGE_WITH_SOURCE;

import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.enums.ActivityMode;
import com.danielvm.destiny2bot.service.WeeklyActivitiesService;
import com.danielvm.destiny2bot.util.MessageUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WeeklyDungeonMessageCreator implements MessageResponseFactory {

  public static final String MESSAGE_TEMPLATE = """
      This week's dungeon is: %s.
      You have until %s to complete it before the next dungeon in the rotation.
      """;
  private final WeeklyActivitiesService weeklyActivitiesService;

  public WeeklyDungeonMessageCreator(WeeklyActivitiesService weeklyActivitiesService) {
    this.weeklyActivitiesService = weeklyActivitiesService;
  }

  @Override
  public Mono<InteractionResponse> createResponse() {
    return weeklyActivitiesService.getWeeklyActivity(ActivityMode.DUNGEON)
        .map(wd -> {
          var endDay = MessageUtil.formatDate(wd.getEndDate().toLocalDate());
          return InteractionResponse.builder()
              .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
              .data(InteractionResponseData.builder()
                  .content(MESSAGE_TEMPLATE.formatted(wd.getName(), endDay))
                  .build())
              .build();
        });
  }
}
