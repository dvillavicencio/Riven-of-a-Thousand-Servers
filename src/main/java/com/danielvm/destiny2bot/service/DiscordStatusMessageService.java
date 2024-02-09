package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.context.IndexingStateContext;
import com.danielvm.destiny2bot.dto.discord.Component;
import com.danielvm.destiny2bot.dto.discord.DmMessageRequest;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.EmbeddedAuthor;
import com.danielvm.destiny2bot.dto.discord.OpenDmChannelRequest;
import com.danielvm.destiny2bot.entity.UserCharacter;
import com.danielvm.destiny2bot.enums.IndexingStatus;
import com.danielvm.destiny2bot.exception.ExternalServiceException;
import com.danielvm.destiny2bot.model.state.ErrorState;
import com.danielvm.destiny2bot.model.state.IndexingState;
import com.danielvm.destiny2bot.model.state.MutableMessagePart;
import com.danielvm.destiny2bot.model.state.NotificationState;
import com.danielvm.destiny2bot.model.state.SuccessState;
import com.danielvm.destiny2bot.util.MessageComponentUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
public class DiscordStatusMessageService {

  private static final String EMBED_STATUS_TITLE = "%s's Raid Data";
  private static final String EMBED_CONTENT_FORMAT = "%s - Light Level %s";

  private final DiscordClient discordClient;
  private final DiscordConfiguration discordConfiguration;
  private final IndexingState indexingState;
  private final ErrorState errorState;
  private final SuccessState successState;

  public DiscordStatusMessageService(
      DiscordClient imperativeDiscordClient,
      DiscordConfiguration discordConfiguration,
      IndexingState indexingState,
      ErrorState errorState,
      SuccessState successState) {
    this.discordClient = imperativeDiscordClient;
    this.discordConfiguration = discordConfiguration;
    this.indexingState = indexingState;
    this.errorState = errorState;
    this.successState = successState;
  }

  private static DmMessageRequest createInitialDmMessage(String discordUsername) {
    Embedded initialEmbedded = Embedded.builder()
        .title(EMBED_STATUS_TITLE.formatted(discordUsername))
        .description("""
            Riven is currently retrieving all your Raid data so that we can gather as much information for you as possible regarding your raids.
            """)
        .author(EmbeddedAuthor.builder()
            .name("Riven of a Thousand Servers")
            .build())
        .build();

    Component informationButton = Component.builder()
        .type(1)
        .components(List.of(Component.builder()
            .customId(MessageComponentUtil.WIRD_BUTTON_ID)
            .type(2)
            .style(1)
            .label("What is Riven doing?")
            .build()))
        .build();

    return DmMessageRequest.builder()
        .embeds(List.of(initialEmbedded))
        .components(List.of(informationButton))
        .build();
  }

  private String updateLoadingContent(Long characterId,
      MutableMessagePart mutableMessagePart, String currentDescription) {
    String updatedContent;
    if (IndexingStateContext.getContext().getSeenCharacters().contains(characterId)) {
      String[] tokens = currentDescription.split("\n");
      tokens[tokens.length - 1] = "%s %s".formatted(mutableMessagePart.getEmoji(),
          mutableMessagePart.getStatefulContent());
      updatedContent = String.join("\n", tokens);
    } else {
      updatedContent = currentDescription + "\n" + "%s %s".formatted(mutableMessagePart.getEmoji(),
          mutableMessagePart.getStatefulContent());
      IndexingStateContext.getContext().getSeenCharacters().add(characterId);
    }
    return updatedContent;
  }

  /**
   * Sends the initial indexing status message for a user by opening a Discord DM Channel and
   * sending the initial payload. This message will be modified until the indexing process finishes
   *
   * @param discordUserId   The DiscordId of the user to initialize a context for
   * @param discordUsername The username of the Discord user
   */
  public void initializeStatusMessage(Long discordUserId, String discordUsername) {
    String botToken = "Bot " + discordConfiguration.getBotToken();
    var channelInfo = discordClient.openDmChannel(botToken, new OpenDmChannelRequest(discordUserId))
        .getBody();

    Assert.notNull(channelInfo,
        "Channel information received was null for user [%s]".formatted(discordUserId));

    DmMessageRequest initialMessage = createInitialDmMessage(discordUsername);

    var response = discordClient.sendDmMessage(channelInfo.getId(), botToken, initialMessage);
    if (response.getStatusCode().is4xxClientError() || response.getStatusCode()
        .is5xxServerError()) {
      String errorMessage = "There was an error when sending the initial status message to user [%s]"
          .formatted(discordUsername);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }

    IndexingStateContext.getContext().setCurrentDmMessage(initialMessage);
    IndexingStateContext.getContext().setMessageId(response.getBody().getId());
    IndexingStateContext.getContext().setChannelId(response.getBody().getChannelId());
  }

  /**
   * Notify the owner of the character that the bot is currently indexing this character
   *
   * @param character The user character that's being indexed
   */
  public void updateStatusMessage(UserCharacter character, IndexingStatus status) {
    NotificationState state = switch (status) {
      case INDEXING -> indexingState;
      case SUCCESS -> successState;
      case ERROR -> errorState;
    };
    MutableMessagePart mutableMessagePart = new MutableMessagePart();
    String username = character.getBotUser().getDiscordUsername();
    String characterContent = EMBED_CONTENT_FORMAT.formatted(character.getDestinyClass(),
        character.getLightLevel());
    mutableMessagePart.setStatefulContent(characterContent);
    state.updateState(mutableMessagePart);
    updateStatusMessage(character.getCharacterId(), mutableMessagePart);
    state.logStatus(username, characterContent, character.getCharacterId());
  }

  private void updateStatusMessage(Long characterId, MutableMessagePart mutableMessagePart) {
    String botToken = "Bot " + discordConfiguration.getBotToken();

    String currentDescription =
        IndexingStateContext.getContext().getCurrentDmMessage().getEmbeds().get(0).getDescription();

    String updatedContent = updateLoadingContent(characterId, mutableMessagePart,
        currentDescription);

    IndexingStateContext.getContext().getCurrentDmMessage().getEmbeds().get(0)
        .setDescription(updatedContent);

    discordClient.updateDmMessage(IndexingStateContext.getContext().getChannelId(),
        IndexingStateContext.getContext().getMessageId(), botToken,
        IndexingStateContext.getContext().getCurrentDmMessage());
  }

  /**
   * Add a finishing message to the indexing state message and clear out all the context data that
   * was used for indexing characters
   */
  public void closeStatusMessage() {
    String botToken = "Bot " + discordConfiguration.getBotToken();
    DmMessageRequest payload = IndexingStateContext.getContext().getCurrentDmMessage();
    Long messageId = IndexingStateContext.getContext().getMessageId();
    Long channelId = IndexingStateContext.getContext().getChannelId();

    String updatedContent =
        payload.getEmbeds().get(0).getDescription()
        + """
                      
                    
            Looks like we're done here! Feel free to use the Bot normally guardian.""";
    payload.getEmbeds().get(0).setDescription(updatedContent);

    discordClient.updateDmMessage(channelId, messageId, botToken, payload);

    IndexingStateContext.clear();
  }

}
