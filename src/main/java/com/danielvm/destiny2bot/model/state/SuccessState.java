package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SuccessState implements NotificationState {

  private static final String SUCCESS_EMOJI_CODE = ":white_check_mark:";

  @Override
  public void updateState(MutableMessagePart mutableMessagePart) {
    mutableMessagePart.setEmoji(SUCCESS_EMOJI_CODE);
  }

  @Override
  public void logStatus(String user, String character, Long characterId) {
    log.info("Successfully indexed character [{}] with Id [{}] for user [{}]", character,
        characterId, user);
  }
}
