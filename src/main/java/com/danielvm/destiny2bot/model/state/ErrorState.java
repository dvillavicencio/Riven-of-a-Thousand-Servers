package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorState {

  private static final String ERROR_EMOJI_CODE = ":white_check_mark:";

  public static void updateState(MutableMessagePart statefulContent) {
    statefulContent.setEmoji(ERROR_EMOJI_CODE);
  }

  public static void logStatus(Long user, String character, Long characterId) {
    log.error("There was an error trying to index character [{}] with Id [{}] for user [{}]",
        character, character, user);
  }
}
