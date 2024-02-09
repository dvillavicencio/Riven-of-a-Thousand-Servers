package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ErrorState implements NotificationState {

  private static final String ERROR_EMOJI_CODE = ":white_check_mark:";

  @Override
  public void updateState(MutableMessagePart statefulContent) {
    statefulContent.setEmoji(ERROR_EMOJI_CODE);
  }

  @Override
  public void logStatus(String user, String character, Long characterId) {
    log.error("There was an error trying to index character [{}] with Id [{}] for user [{}]",
        character, character, user);
  }
}
