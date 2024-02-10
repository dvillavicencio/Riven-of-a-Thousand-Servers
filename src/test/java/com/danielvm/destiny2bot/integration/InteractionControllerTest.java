package com.danielvm.destiny2bot.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.milestone.MilestoneEntry;
import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionData;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.Option;
import com.danielvm.destiny2bot.enums.InteractionType;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.enums.Raid;
import com.danielvm.destiny2bot.enums.RaidEncounter;
import com.danielvm.destiny2bot.util.MessageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

public class InteractionControllerTest extends BaseIntegrationTest {

  // Static mapper to be used on the @BeforeAll static method
  private static final ObjectMapper OBJECT_MAPPER = new JsonMapper.Builder(new JsonMapper())
      .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .build()
      .registerModule(new JavaTimeModule());

  @Autowired
  UserDetailsReactiveDao userDetailsReactiveDao;

  /**
   * This method replaces all the placeholder values in the milestones-response.json file The reason
   * for this is that the /weekly_raid and /weekly_dungeon responses will be weird if the dates are
   * not dynamic, therefore this method
   *
   * @throws IOException in case we are not able to write back to the file (in-place)
   */
  @BeforeAll
  public static void before() throws IOException {
    File milestoneFile = new File("src/test/resources/__files/bungie/milestone-response.json");
    TypeReference<BungieResponse<Map<String, MilestoneEntry>>> typeReference = new TypeReference<>() {
    };
    var milestoneResponse = OBJECT_MAPPER.readValue(milestoneFile, typeReference);

    replaceDates(milestoneResponse, "526718853");
    replaceDates(milestoneResponse, "2712317338");

    OBJECT_MAPPER.writeValue(milestoneFile, milestoneResponse);
  }

  private static void replaceDates(BungieResponse<Map<String, MilestoneEntry>> response,
      String hash) {
    response.getResponse().entrySet().stream()
        .filter(entry -> Objects.equals(entry.getKey(), hash))
        .forEach(entry -> {
          var startDate = entry.getValue().getStartDate();
          var endDate = entry.getValue().getEndDate();
          if (Objects.nonNull(startDate)) {
            entry.getValue().setStartDate(MessageUtil.PREVIOUS_TUESDAY);
          }
          if (Objects.nonNull(endDate)) {
            entry.getValue().setEndDate(MessageUtil.NEXT_TUESDAY);
          }
        });
  }

  @Test
  @DisplayName("get weekly dungeon works successfully")
  public void getWeeklyDungeonWorksSuccessfully() throws JsonProcessingException, DecoderException {
    // given: a weekly_dungeon interaction with a valid signature
    InteractionData weeklyDungeonData = InteractionData.builder()
        .id(2).name("weekly_dungeon").type(1)
        .build();
    Interaction body = Interaction.builder().id(1)
        .applicationId("theApplicationId").data(weeklyDungeonData).type(2)
        .build();

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/milestone-response.json")));

    var activityDefinition = ManifestEntity.ACTIVITY_DEFINITION.getId();
    var activityHash = "1262462921";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, activityHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-activity-response.json")));

    var masterDungeonHash = "2296818662";
    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, masterDungeonHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/master-dungeon-activity-response.json")));

    var activityTypeDefinition = ManifestEntity.ACTIVITY_TYPE_DEFINITION.getId();
    var activityTypeHash = "608898761";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityTypeDefinition, activityTypeHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-activity-type-response.json")));

    var milestoneDefinition = ManifestEntity.MILESTONE_DEFINITION.getId();
    var milestoneHash = "526718853";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(milestoneDefinition, milestoneHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-milestone-response.json")));

    // when: the request is sent
    var response = sendValidSignatureRequest("/interactions", body);

    // then: the response JSON is correct
    response.expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.type").isEqualTo(4)
        .jsonPath("$.data.content").isEqualTo(
            """
                This week's dungeon is: Spire of the Watcher.
                You have until %s to complete it before the next dungeon in the rotation.
                """.formatted(MessageUtil.formatDate(MessageUtil.NEXT_TUESDAY.toLocalDate())));
  }

  @Test
  @DisplayName("get weekly raid works successfully")
  public void getWeeklyRaidWorksSuccessfully() throws JsonProcessingException, DecoderException {
    // given: a weekly_raid interaction with a valid signature
    InteractionData weeklyRaidData = InteractionData.builder()
        .id(2).name("weekly_raid").type(1)
        .build();
    Interaction body = Interaction.builder().id(1).applicationId("theApplicationId").type(2)
        .data(weeklyRaidData).build();

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/milestone-response.json")));

    var activityDefinition = ManifestEntity.ACTIVITY_DEFINITION.getId();
    var activityHash = "1042180643";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, activityHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-activity-response.json")));

    var activityTypeDefinition = ManifestEntity.ACTIVITY_TYPE_DEFINITION.getId();
    var activityTypeHash = "2043403989";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityTypeDefinition, activityTypeHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-activity-type-response.json")));

    var milestoneDefinition = ManifestEntity.MILESTONE_DEFINITION.getId();
    var milestoneHash = "2712317338";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(milestoneDefinition, milestoneHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-milestone-response.json")));

    // when: the request is sent
    var response = sendValidSignatureRequest("/interactions", body);

    // then: the response JSON is correct
    response.expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.type").isEqualTo(4)
        .jsonPath("$.data.content").isEqualTo(
            """
                This week's raid is: Garden of Salvation.
                You have until %s to complete it before the next raid comes along.
                """.formatted(MessageUtil.formatDate(MessageUtil.NEXT_TUESDAY.toLocalDate())));
  }

  @Test
  @DisplayName("get weekly raid fails if no milestones are found")
  public void getWeeklyRaidsShouldThrowErrors() throws JsonProcessingException, DecoderException {
    // given: a weekly_raid interaction with a valid signature
    InteractionData weeklyRaidData = InteractionData.builder()
        .id(2).name("weekly_raid").type(1)
        .build();
    Interaction body = Interaction.builder().id(1).applicationId("theApplicationId").type(2)
        .data(weeklyRaidData).build();

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getApiKey()))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/missing-api-key.json")));

    // when: the request is sent
    var response = sendValidSignatureRequest("/interactions", body);

    // then: the response JSON is correct
    String errorJson;
    try {
      errorJson = objectMapper.writeValueAsString(
          objectMapper.readValue(
              new ClassPathResource("__files/bungie/missing-api-key.json").getInputStream(),
              Object.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    response.expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.detail").value(json -> assertJsonLenient(errorJson, json))
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @DisplayName("Interactions fail if the signature is invalid")
  public void getWeeklyRaidInvalidSignature() throws JsonProcessingException, DecoderException {
    // given: an interaction with an invalid signature
    InteractionData data = InteractionData.builder()
        .id(2).name("weekly_raid").type(1).build();
    Interaction body = Interaction.builder()
        .id(1).applicationId("theApplicationId").type(2).data(data)
        .build();

    // when: the request is sent
    ResponseSpec response = sendInvalidSignatureRequest("/interactions", body);

    // then: the response JSON has the correct error message
    response.expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value());

  }

  @Test
  @DisplayName("PING interactions with valid signatures are ack'd correctly")
  public void pingRequestsAreAckdCorrectly() throws JsonProcessingException, DecoderException {
    // given: an interaction with an invalid signature
    Interaction body = Interaction.builder().id(1).applicationId("theApplicationId").type(1)
        .build();

    // when: the request is sent
    var response = sendValidSignatureRequest("/interactions", body);

    // then: the response JSON has the correct error message
    response.expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.type").isEqualTo(InteractionType.PING.getType());
  }

  @Test
  @DisplayName("PING interactions with invalid signatures are not ack'd")
  public void invalidPingRequestsAreNotAckd() throws JsonProcessingException, DecoderException {
    // given: an interaction with an invalid signature
    Interaction body = Interaction.builder().id(1)
        .applicationId("theApplicationId").type(1)
        .build();

    // when: the request is sent
    var response = sendInvalidSignatureRequest("/interactions", body);

    // then: the response JSON has the correct error message
    response.expectStatus()
        .isBadRequest()
        .expectBody()
        .consumeWith(System.out::println)
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @DisplayName("Autocomplete requests for a raid map request is successful")
  public void autocompleteRequestsForRaidMapAreSuccessful()
      throws DecoderException, IOException {
    // given: an interaction for autocomplete for the /raid_map command
    List<Option> options = List.of(
        new Option("raid", 3, "last_wish", false),
        new Option("raid", 3, "k", true)
    );
    InteractionData data = InteractionData.builder()
        .id("someID")
        .name("raid_map")
        .options(options).build();
    Interaction body = Interaction.builder()
        .id("someInteractionID")
        .data(data)
        .type(InteractionType.APPLICATION_COMMAND_AUTOCOMPLETE.getType())
        .build();

    // when: the raid_map autocomplete interaction is requested
    var response = sendValidSignatureRequest("/interactions", body);

    // then: the correct raid encounters options are returned
    List<Choice> expectedChoices = RaidEncounter.getRaidEncounters(Raid.LAST_WISH)
        .map(raidEncounter -> new Choice(raidEncounter.getName(), raidEncounter.getDirectory()))
        .toStream().toList();

    byte[] responseBody = response.expectStatus().is2xxSuccessful()
        .expectBody()
        .consumeWith(System.out::println)
        .jsonPath("$.type").isEqualTo(8)
        .jsonPath("$.data.choices.size()").isEqualTo(6)
        .jsonPath("$.data.choices").isArray()
        .returnResult().getResponseBody();

    InteractionResponse interactionResponse = objectMapper.readValue(responseBody,
        InteractionResponse.class);

    assertThat(interactionResponse.getData().getChoices()).containsAll(expectedChoices);
  }

  @Test
  @DisplayName("Command request for raid map request is for 1 image")
  public void commandRequestsForRaidMapAreSuccessful()
      throws DecoderException, IOException {
    // given: an interaction for autocomplete for the /raid_map command
    List<Option> options = List.of(
        new Option("raid", 3, "last_wish", false),
        new Option("encounter", 3, "kalli", false)
    );
    InteractionData data = InteractionData.builder()
        .id("someID")
        .name("raid_map")
        .options(options).build();
    Interaction body = Interaction.builder()
        .id("someInteractionID")
        .data(data)
        .type(InteractionType.APPLICATION_COMMAND.getType())
        .build();

    // when: the raid_map autocomplete interaction is requested
    var response = sendValidSignatureRequest("/interactions", body);

    // TODO: Revisit this integration test for assertions on the multipart parts
    // then: the correct raid encounters options are returned
    response.expectStatus().is2xxSuccessful()
        .expectHeader().value("Content-Type", containsString("multipart/form-data"));

  }

}