package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IndexingState implements NotificationState {

  private static final String LOADER_EMOJI_CODE = "<a:loader:1202857581169610803>";

  @Override
  public void logStatus(String user, String character, Long characterId) {
    log.info("Current character [{}] with id [{}] is on status [indexing] for user [{}]", character,
        characterId, user);
  }

  @Override
  public void updateState(MutableMessagePart messagePart) {
    messagePart.setEmoji(LOADER_EMOJI_CODE);
  }
}
