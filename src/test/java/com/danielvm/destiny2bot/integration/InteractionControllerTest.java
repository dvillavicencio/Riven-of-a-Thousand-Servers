package com.danielvm.destiny2bot.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.destiny.GenericResponse;
import com.danielvm.destiny2bot.dto.destiny.milestone.MilestoneEntry;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionData;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.enums.InteractionType;
import com.danielvm.destiny2bot.util.MessageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import software.pando.crypto.nacl.Crypto;

// TODO: Write integration tests for autocomplete features
public class InteractionControllerTest extends BaseIntegrationTest {

  private static final String VALID_PRIVATE_KEY = "F0EA3A0516695324C03ED552CD5A08A58CA1248172E8816C3BF235E52E75A7BF";
  private static final String MALICIOUS_PRIVATE_KEY = "CE4517095255B0C92D586AF9EEC27B998D68775363F9FE74341483FB3A657CEC";

  // Static mapper to be used on the @BeforeAll static method
  private static final ObjectMapper OBJECT_MAPPER = new JsonMapper.Builder(new JsonMapper())
      .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .build()
      .registerModule(new JavaTimeModule());

  @Autowired
  BungieConfiguration bungieConfiguration;
  @Autowired
  DiscordConfiguration discordConfiguration;

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
    TypeReference<GenericResponse<Map<String, MilestoneEntry>>> typeReference = new TypeReference<>() {
    };
    var milestoneResponse = OBJECT_MAPPER.readValue(milestoneFile, typeReference);

    replaceDates(milestoneResponse, "526718853");
    replaceDates(milestoneResponse, "2712317338");

    OBJECT_MAPPER.writeValue(milestoneFile, milestoneResponse);
  }

  private static void replaceDates(GenericResponse<Map<String, MilestoneEntry>> response,
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

  private String createValidSignature(Interaction body, String timestamp)
      throws JsonProcessingException, DecoderException {
    KeyPair signingKeys = Crypto.seedSigningKeyPair(Hex.decodeHex(VALID_PRIVATE_KEY.toCharArray()));

    discordConfiguration.setBotPublicKey(
        Hex.encodeHexString(
            signingKeys.getPublic().getEncoded())); // change the public key in the config class

    var signatureBytes = Crypto.sign(signingKeys.getPrivate(),
        (timestamp + objectMapper.writeValueAsString(body)).getBytes(StandardCharsets.UTF_8));
    return Hex.encodeHexString(signatureBytes);
  }

  private String createInvalidSignature(Interaction body, String timestamp)
      throws JsonProcessingException, DecoderException {
    KeyPair invalidSigningKeyPair = Crypto.seedSigningKeyPair(
        Hex.decodeHex(MALICIOUS_PRIVATE_KEY.toCharArray()));

    PublicKey validPublicKey = Crypto.seedSigningKeyPair(
        Hex.decodeHex(VALID_PRIVATE_KEY.toCharArray())).getPublic();

    discordConfiguration.setBotPublicKey(
        Hex.encodeHexString(validPublicKey.getEncoded()));

    var signatureBytes = Crypto.sign(invalidSigningKeyPair.getPrivate(),
        (timestamp + objectMapper.writeValueAsString(body)).getBytes(StandardCharsets.UTF_8));
    return Hex.encodeHexString(signatureBytes);
  }

  @Test
  @DisplayName("get weekly dungeon works successfully")
  public void getWeeklyDungeonWorksSuccessfully() throws JsonProcessingException, DecoderException {
    // given: a weekly_dungeon interaction with a valid signature
    InteractionData weeklyDungeonData = new InteractionData(2, "weekly_dungeon", 1);
    Interaction body = new Interaction(1, "theApplicationId", 2, weeklyDungeonData, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createValidSignature(body, timestamp);

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/milestone-response.json")));

    var activityDefinition = ManifestEntity.ACTIVITY_DEFINITION.getId();
    var activityHash = "1262462921";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, activityHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-activity-response.json")));

    var masterDungeonHash = "2296818662";
    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, masterDungeonHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/master-dungeon-activity-response.json")));

    var activityTypeDefinition = ManifestEntity.ACTIVITY_TYPE_DEFINITION.getId();
    var activityTypeHash = "608898761";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityTypeDefinition, activityTypeHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-activity-type-response.json")));

    var milestoneDefinition = ManifestEntity.MILESTONE_DEFINITION.getId();
    var milestoneHash = "526718853";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(milestoneDefinition, milestoneHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/dungeon-milestone-response.json")));

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

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
    InteractionData weeklyRaidData = new InteractionData(2, "weekly_raid", 1);
    Interaction body = new Interaction(1, "theApplicationId", 2, weeklyRaidData, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createValidSignature(body, timestamp);

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/milestone-response.json")));

    var activityDefinition = ManifestEntity.ACTIVITY_DEFINITION.getId();
    var activityHash = "1042180643";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityDefinition, activityHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-activity-response.json")));

    var activityTypeDefinition = ManifestEntity.ACTIVITY_TYPE_DEFINITION.getId();
    var activityTypeHash = "2043403989";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(activityTypeDefinition, activityTypeHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-activity-type-response.json")));

    var milestoneDefinition = ManifestEntity.MILESTONE_DEFINITION.getId();
    var milestoneHash = "2712317338";

    stubFor(get(urlPathEqualTo(
        "/bungie/Destiny2/Manifest/%s/%s/".formatted(milestoneDefinition, milestoneHash)))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/raid-milestone-response.json")));

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

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
    InteractionData weeklyRaidData = new InteractionData(2, "weekly_raid", 1);
    Interaction body = new Interaction(1, "theApplicationId", 2, weeklyRaidData, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createValidSignature(body, timestamp);

    stubFor(get(urlPathEqualTo("/bungie/Destiny2/Milestones/"))
        .withHeader("x-api-key", equalTo(bungieConfiguration.getKey()))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/missing-api-key.json")));

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

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
    InteractionData data = new InteractionData(2, "weekly_raid", 1);
    Interaction body = new Interaction(1, "theApplicationId", 2, data, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createInvalidSignature(body, timestamp);

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

    // then: the response JSON has the correct error message
    response.expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
        .jsonPath("$.detail").isEqualTo("interactions.request: Signature is invalid");
  }

  @Test
  @DisplayName("PING interactions with valid signatures are ack'd correctly")
  public void pingRequestsAreAckdCorrectly() throws JsonProcessingException, DecoderException {
    // given: an interaction with an invalid signature
    Interaction body = new Interaction(1, "theApplicationId", 1, null, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createValidSignature(body, timestamp);

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

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
    Interaction body = new Interaction(1, "theApplicationId", 1, null, null);
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createInvalidSignature(body, timestamp);

    // when: the request is sent
    var response = webTestClient.post()
        .uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(body))
        .exchange();

    // then: the response JSON has the correct error message
    response.expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
        .jsonPath("$.detail").isEqualTo("interactions.request: Signature is invalid");
  }
}