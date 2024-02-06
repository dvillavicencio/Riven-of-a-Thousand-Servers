package com.danielvm.destiny2bot.model.state;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SuccessState {

  private static final String SUCCESS_EMOJI_CODE = ":white_check_mark:";

  public static void updateState(MutableMessagePart mutableMessagePart) {
    mutableMessagePart.setEmoji(SUCCESS_EMOJI_CODE);
  }

  public static void logStatus(Long user, String character, Long characterId) {
    log.info("Finished indexing character [{}] with Id [{}] for user [{}]", character, characterId,
        user);
  }
}
