package com.danielvm.destiny2bot.context;

import com.danielvm.destiny2bot.dto.discord.DmMessageRequest;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IndexingStateContext {

  private static final ThreadLocal<IndexingStateContext> STATE_CONTEXT = ThreadLocal.withInitial(
      IndexingStateContext::new);

  private final Set<Long> seenCharacters = new HashSet<>();

  private DmMessageRequest currentDmMessage;
  private Long channelId;
  private Long messageId;

  private IndexingStateContext() {

  }

  public static IndexingStateContext getContext() {
    return STATE_CONTEXT.get();
  }

  public static void clear() {
    IndexingStateContext context = STATE_CONTEXT.get();
    context.seenCharacters.clear();
    context.currentDmMessage = null;
    context.channelId = null;
    context.messageId = null;
  }
}
