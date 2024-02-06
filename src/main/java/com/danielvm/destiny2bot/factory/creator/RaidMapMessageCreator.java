package com.danielvm.destiny2bot.factory.creator;

import com.danielvm.destiny2bot.dto.discord.Attachment;
import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.Component;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.EmbeddedImage;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.dto.discord.Option;
import com.danielvm.destiny2bot.enums.InteractionResponseType;
import com.danielvm.destiny2bot.enums.Raid;
import com.danielvm.destiny2bot.enums.RaidEncounter;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.service.ImageAssetService;
import com.danielvm.destiny2bot.util.InteractionUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@org.springframework.stereotype.Component
public class RaidMapMessageCreator implements ApplicationCommandSource, AutocompleteSource {

  private static final String EMBED_BINDING_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
  private static final String RAID_OPTION_NAME = "raid";
  private static final String ENCOUNTER_OPTION_NAME = "encounter";

  private final ImageAssetService imageAssetService;

  public RaidMapMessageCreator(
      ImageAssetService imageAssetService) {
    this.imageAssetService = imageAssetService;
  }

  private static InteractionResponse formatInteractionResponse(
      Interaction interaction, List<Attachment> attachments) {
    Assert.notNull(interaction.getData().getOptions(),
        "The options for the interaction cannot be null");
    List<Option> interactionOptions = interaction.getData().getOptions();

    String encounterDirectory = InteractionUtil.retrieveInteractionOption(
        interactionOptions, ENCOUNTER_OPTION_NAME);
    String raidDirectory = InteractionUtil.retrieveInteractionOption(
        interactionOptions, RAID_OPTION_NAME);

    Raid raid = Raid.findRaid(raidDirectory);

    RaidEncounter raidEncounter = RaidEncounter.findEncounter(raid, encounterDirectory);

    String embedTitle = """
        Encounter maps for: %s at %s""".formatted(raid.getRaidName(), raidEncounter.getName());
    List<Embedded> embeds = attachments.stream()
        .map(attachment -> Embedded.builder()
            .title(embedTitle)
            .url(EMBED_BINDING_URL)
            .type("image")
            .image(EmbeddedImage.builder()
                .url("attachment://" + attachment.getFilename())
                .build())
            .build())
        .toList();
    InteractionResponseData data = InteractionResponseData.builder()
        .components(List.of(Component.builder()
            .type(1)
            .components(List.of(Component.builder()
                .label("Infographics by A-Phantom-Moon!")
                .type(2)
                .style(5)
                .url("https://www.deviantart.com/a-phantom-moon")
                .build()))
            .build()))
        .attachments(attachments)
        .embeds(embeds)
        .build();
    return InteractionResponse.builder()
        .type(InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(data)
        .build();
  }

  private static List<Attachment> extractAttachments(Map<Long, Resource> map) {
    return map.entrySet().stream()
        .map(entry -> {
          try {
            return Attachment.builder()
                .id(entry.getKey())
                .filename(entry.getValue().getFilename())
                .size(Math.toIntExact(entry.getValue().contentLength()))
                .build();
          } catch (IOException e) {
            String errorMessage = "Something went wrong while parsing filename for resource [%s]"
                .formatted(entry.getValue());
            throw new InternalServerException(errorMessage, e);
          }
        })
        .toList();
  }

  @Override
  public Mono<InteractionResponse> createResponse(Interaction interaction) {
    try {
      return imageAssetService.retrieveEncounterImages(interaction)
          .map(RaidMapMessageCreator::extractAttachments)
          .map(attachments -> formatInteractionResponse(interaction, attachments));
    } catch (IOException e) {
      String raidName = InteractionUtil.retrieveInteractionOption(interaction.getData()
          .getOptions(), ENCOUNTER_OPTION_NAME);
      String errorMessage =
          "Something wrong happened while retrieving encounter images for raid [%s]".formatted(
              raidName);
      log.error(errorMessage, e);
      throw new InternalServerException(errorMessage, e);
    }
  }

  @Override
  public Mono<InteractionResponse> autocompleteResponse(Interaction interaction) {
    return Flux.fromIterable(interaction.getData().getOptions())
        .filter(option ->
            Objects.equals(option.getName(), RAID_OPTION_NAME) &&
            Objects.equals(option.getType(), 3)) // 3 is for String data type
        .flatMap(option ->
            Flux.fromIterable(Arrays.asList(Raid.values()))
                .filter(raid -> raid.getRaidDirectory()
                    .equalsIgnoreCase(String.valueOf(option.getValue())))
                .next())
        .flatMap(raid ->
            Mono.just(raid)
                .flatMapMany(RaidEncounter::getRaidEncounters)
                .map(encounter ->
                    new Choice(encounter.getName(), encounter.getDirectory()))
        )
        .collectList()
        .map(encounters ->
            InteractionResponse.builder()
                .type(InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT.getType())
                .data(InteractionResponseData.builder()
                    .choices(encounters)
                    .build())
                .build());
  }
}
