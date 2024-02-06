package com.danielvm.destiny2bot.context;

import com.danielvm.destiny2bot.dto.discord.DmMessageRequest;
import java.util.HashSet;
import java.util.Set;

public class IndexingStateContext {

  private static final ThreadLocal<IndexingStateContext> STATE_CONTEXT = ThreadLocal.withInitial(
      IndexingStateContext::new);

  private final Set<Long> seenCharacters = new HashSet<>();

  private DmMessageRequest payload;
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
    context.payload = null;
    context.channelId = null;
    context.messageId = null;
  }

  public DmMessageRequest getCurrentPayload() {
    return this.payload;
  }

  public void setPayload(DmMessageRequest payload) {
    this.payload = payload;
  }

  public Long getChannelId() {
    return channelId;
  }

  public void setChannelId(Long channelId) {
    this.channelId = channelId;
  }

  public Long getMessageId() {
    return messageId;
  }

  public void setMessageId(Long messageId) {
    this.messageId = messageId;
  }

  public Set<Long> getSeenCharacters() {
    return seenCharacters;
  }
}
