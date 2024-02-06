package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.context.IndexingStateContext;
import com.danielvm.destiny2bot.dto.discord.Component;
import com.danielvm.destiny2bot.dto.discord.DmMessageRequest;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.EmbeddedAuthor;
import com.danielvm.destiny2bot.dto.discord.OpenDmChannelRequest;
import com.danielvm.destiny2bot.exception.ExternalServiceException;
import com.danielvm.destiny2bot.model.state.MutableMessagePart;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
public class IndexingStatusService {

  private static final String EMBED_STATUS_TITLE = "%s's Raid Data";

  private final DiscordClient discordClient;
  private final DiscordConfiguration discordConfiguration;

  public IndexingStatusService(DiscordClient imperativeDiscordClient,
      DiscordConfiguration discordConfiguration) {
    this.discordClient = imperativeDiscordClient;
    this.discordConfiguration = discordConfiguration;
  }

  private String updateCharacterLoadingContent(Long characterId,
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
   * Prepare the indexing status notification for a user by opening a Discord DM Channel and sending
   * the initial payload. This should ALWAYS be called before sending
   *
   * @param discordUserId   The DiscordId of the user to initialize a context for
   * @param discordUsername The username of the Discord user
   */
  public void prepareIndexingStatusMessage(Long discordUserId,
      String discordUsername) {
    String botToken = "Bot " + discordConfiguration.getBotToken();
    var channelInfo = discordClient.openDmChannel(botToken, new OpenDmChannelRequest(discordUserId))
        .getBody();

    Assert.notNull(channelInfo,
        "Channel information received was null for user [%s]".formatted(discordUserId));

    // Send the initial embedded message
    Embedded initialEmbedded = Embedded.builder()
        .title(EMBED_STATUS_TITLE.formatted(discordUsername))
        .description(
            "Riven is currently retrieving all your Raid data so that we can"
            + " gather as much information for you as possible regarding your raids.")
        .author(EmbeddedAuthor.builder()
            .name("Riven of a Thousand Servers")
            .build())
        .build();

    Component informationButton = Component.builder()
        .type(1)
        .components(List.of(Component.builder()
            .customId("WIRD")
            .type(2)
            .style(1)
            .label("What is Riven doing?")
            .build()))
        .build();

    DmMessageRequest payload = DmMessageRequest.builder()
        .embeds(List.of(initialEmbedded))
        .components(List.of(informationButton))
        .build();

    var response = discordClient.sendDmMessage(channelInfo.getId(), botToken, payload);
    if (response.getStatusCode().is4xxClientError() || response.getStatusCode()
        .is5xxServerError()) {
      String errorMessage = "There was an error when sending the initial status message to user [%s]"
          .formatted(discordUsername);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }

    IndexingStateContext.getContext().setPayload(payload);
    IndexingStateContext.getContext().setMessageId(response.getBody().getId());
    IndexingStateContext.getContext().setChannelId(response.getBody().getChannelId());
  }

  /**
   * Update the current state of the indexing message
   *
   * @param characterId        The current character that's being indexed
   * @param mutableMessagePart The content that is changing
   */
  public void updateState(Long characterId, MutableMessagePart mutableMessagePart) {
    String botToken = "Bot " + discordConfiguration.getBotToken();

    String currentDescription =
        IndexingStateContext.getContext().getCurrentPayload().getEmbeds().get(0).getDescription();

    String updatedContent = updateCharacterLoadingContent(characterId, mutableMessagePart,
        currentDescription);

    IndexingStateContext.getContext().getCurrentPayload().getEmbeds().get(0)
        .setDescription(updatedContent);

    discordClient.updateDmMessage(IndexingStateContext.getContext().getChannelId(),
        IndexingStateContext.getContext().getMessageId(), botToken,
        IndexingStateContext.getContext().getCurrentPayload());
  }

  /**
   * Add a finishing message to the indexing state message and clear out all the context data that
   * was used for indexing characters
   */
  public void finishIndexing() {
    String botToken = "Bot " + discordConfiguration.getBotToken();
    DmMessageRequest payload = IndexingStateContext.getContext().getCurrentPayload();
    String updatedContent =
        payload.getEmbeds().get(0).getDescription()
        + "\n\nLooks like we're done here! Feel free to use the Bot normally guardian.";
    payload.getEmbeds().get(0).setDescription(updatedContent);
    discordClient.updateDmMessage(IndexingStateContext.getContext().getChannelId(),
        IndexingStateContext.getContext().getMessageId(), botToken, payload);

    IndexingStateContext.clear();
  }

}
