package com.danielvm.destiny2bot.factory;

import static com.danielvm.destiny2bot.enums.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.danielvm.destiny2bot.ResourceUtils;
import com.danielvm.destiny2bot.dto.discord.Attachment;
import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.Embedded;
import com.danielvm.destiny2bot.dto.discord.EmbeddedImage;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionData;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.dto.discord.Option;
import com.danielvm.destiny2bot.enums.InteractionResponseType;
import com.danielvm.destiny2bot.enums.InteractionType;
import com.danielvm.destiny2bot.enums.Raid;
import com.danielvm.destiny2bot.enums.RaidEncounter;
import com.danielvm.destiny2bot.factory.creator.RaidMapMessageCreator;
import com.danielvm.destiny2bot.service.ImageAssetService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class RaidMapMessageCreatorTest {

  @Mock
  private ImageAssetService imageAssetService;

  @InjectMocks
  private RaidMapMessageCreator sut;

  @Test
  @DisplayName("Creating autocomplete response works correctly")
  public void creatingAutocompleteResponseWorksCorrectly() {
    // given: an interaction with a 'raid' option value present
    List<Option> options = List.of(new Option("raid", 3, "last_wish", false));
    InteractionData data = InteractionData.builder()
        .id(1).name("raid_map").options(options).type(1)
        .build();
    Interaction interaction = Interaction.builder()
        .id(1).data(data).type(InteractionType.APPLICATION_COMMAND_AUTOCOMPLETE.getType())
        .build();

    List<Choice> choices = RaidEncounter.getRaidEncounters(Raid.LAST_WISH)
        .map(encounter -> new Choice(encounter.getName(), encounter.getDirectory()))
        .collectList().block();
    InteractionResponseData responseData = InteractionResponseData.builder()
        .choices(choices)
        .build();
    InteractionResponse expectedOutput = InteractionResponse.builder()
        .type(InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT.getType())
        .data(responseData)
        .build();

    // when: the autocomplete request is received
    var response = StepVerifier.create(sut.autocompleteResponse(interaction));

    // then: the choices presented are correct according to the Raid
    response.assertNext(output -> {
      assertThat(output).isEqualTo(expectedOutput);
    }).verifyComplete();
  }

  @Test
  @DisplayName("Creating application command response works correctly")
  public void creatingApplicationCommandResponseWorksCorrectly() throws IOException {
    // given: an interaction with a 'raid' option value present
    List<Option> options = List.of(
        new Option("raid", 3, "last_wish", false),
        new Option("encounter", 3, "kalli", false));
    InteractionData data = InteractionData.builder()
        .id(1).name("raid_map").options(options).type(1)
        .build();
    Interaction interaction = Interaction.builder()
        .id(1).data(data).type(InteractionType.APPLICATION_COMMAND_AUTOCOMPLETE.getType())
        .build();

    Map<Long, Resource> resourcesMap = Map.of(
        1L, ResourceUtils.createResourceWithName("kalli-action-phase.jpg"),
        2L, ResourceUtils.createResourceWithName("kalli-dps-phase.jpg")
    );

    String raidName = "Last Wish";
    String raidEncounter = "Kalli, the Corrupted";
    String embedTitle = "Encounter maps for: %s at %s".formatted(raidName, raidEncounter);

    List<Embedded> expectedEmbeds = List.of(
        Embedded.builder()
            .title(embedTitle)
            .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .image(EmbeddedImage.builder()
                .url("attachment://kalli-action-phase.jpg")
                .build())
            .type("image").build(),
        Embedded.builder()
            .title(embedTitle)
            .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .image(EmbeddedImage.builder()
                .url("attachment://kalli-dps-phase.jpg")
                .build())
            .type("image").build()
    );

    List<Attachment> expectedAttachments = resourcesMap.entrySet().stream()
        .map(entry -> {
          try {
            return Attachment.builder()
                .id(entry.getKey())
                .filename(entry.getValue().getFilename())
                .size(Math.toIntExact(entry.getValue().contentLength()))
                .build();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).toList();

    Mockito.when(imageAssetService.retrieveEncounterImages(interaction))
        .thenReturn(Mono.just(resourcesMap));

    // when: the autocomplete request is received
    var response = StepVerifier.create(sut.createResponse(interaction));

    // then: the choices presented are correct according to the Raid
    response.assertNext(output -> {
      assertThat(output.getType()).isEqualTo(CHANNEL_MESSAGE_WITH_SOURCE.getType());
      assertThat(output.getData().getEmbeds()).containsAll(expectedEmbeds);
      assertThat(output.getData().getAttachments()).containsAll(expectedAttachments);
    }).verifyComplete();
  }

}
