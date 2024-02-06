package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexingState {

  private static final String LOADER_EMOJI_CODE = "<a:loader:1202857581169610803>";

  public static void updateState(MutableMessagePart statefulContent) {
    statefulContent.setEmoji(LOADER_EMOJI_CODE);
  }

  public static void logStatus(Long user, String character, Long characterId) {
    log.info("Current character [{}] with id [{}] is indexing status for user [{}]", character,
        characterId, user);
  }
}
