package com.danielvm.destiny2bot.factory;

import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.creator.MessageComponentSource;
import com.danielvm.destiny2bot.factory.creator.WIRDButtonMessageCreator;
import com.danielvm.destiny2bot.factory.creator.WhyAuthorizeButtonMessageCreator;
import com.danielvm.destiny2bot.util.MessageComponentUtil;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MessageComponentFactory {

  private final Map<String, MessageComponentSource> messageComponentFactory;

  public MessageComponentFactory(
      WIRDButtonMessageCreator wirdButtonMessageCreator,
      WhyAuthorizeButtonMessageCreator whyAuthorizeButtonMessageCreator) {
    this.messageComponentFactory = Map.of(
        MessageComponentUtil.WIRD_BUTTON_ID, wirdButtonMessageCreator,
        MessageComponentUtil.WHY_AUTHORIZE_BUTTON_ID, whyAuthorizeButtonMessageCreator
    );
  }

  /**
   * Retrieve a message component source creator based on the Id of the message component sent
   *
   * @param customId The ID of the message component
   * @return {@link MessageComponentSource}
   */
  public MessageComponentSource messageCreator(String customId) {
    MessageComponentSource creator = messageComponentFactory.get(customId);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for componentId [%s]".formatted(customId));
    }
    return creator;
  }
}
