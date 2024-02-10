package com.danielvm.destiny2bot.integration;

import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.pando.crypto.nacl.Crypto;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(files = "/build/resources/test/__files")
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

  @Container
  public static final PostgreSQLContainer<?> POSTGRES_SQL_CONTAINER = new PostgreSQLContainer<>(
      "postgres:16.1")
      .withDatabaseName("riven_of_a_thousand_servers")
      .withUsername("username")
      .withPassword("password");

  @Container
  private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(
      "redis:5.0.3-alpine").withExposedPorts(6379);

  private static final String MALICIOUS_PRIVATE_KEY = "CE4517095255B0C92D586AF9EEC27B998D68775363F9FE74341483FB3A657CEC";
  private static final String VALID_PRIVATE_KEY = "F0EA3A0516695324C03ED552CD5A08A58CA1248172E8816C3BF235E52E75A7BF";

  @LocalServerPort
  protected int localServerPort;

  @Autowired
  MockMvc mockMvc;

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  DiscordConfiguration discordConfiguration;

  @Autowired
  BungieConfiguration bungieConfiguration;

  /**
   * Starts up some dynamic properties that change because of TestContainers usage, as well as the
   * test environment having a random port
   *
   * @param registry {@link DynamicPropertyRegistry}
   */
  @DynamicPropertySource
  public static void setup(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
  }

  /**
   * Asserts a Json input is valid/invalid regardless of formatting, spacing, new-lines, etc.
   *
   * @param target   The target json to be compared
   * @param expected The expected json that the target should match
   */
  public static void assertJsonLenient(String target, Object expected) {
    try {
      JSONAssert.assertEquals(expected.toString(), target, false);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends a partial WebTestClient request to the specified route with a valid signature
   *
   * @param endpoint    The /endpoint to call
   * @param interaction The interaction body that the user will send along this request
   * @return {@link WebTestClient.RequestHeadersSpec}
   * @throws DecoderException        If something goes wrong with the Crypto library
   * @throws JsonProcessingException If Jackson is unable to parse the body to Json for signing
   */
  public ResponseSpec sendValidSignatureRequest(String endpoint, Interaction interaction)
      throws DecoderException, JsonProcessingException {
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createValidSignature(interaction, timestamp);
    return this.webTestClient.post().uri(endpoint)
        .accept(MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .body(BodyInserters.fromValue(interaction))
        .exchange();
  }

  /**
   * Sends a WebTestClient request with the appropriate headers but with an invalid signature
   *
   * @param endpoint    The /endpoint to call
   * @param interaction The interaction body that the user will send along this request
   * @return {@link WebTestClient.ResponseSpec}
   * @throws DecoderException        If something goes wrong with the Crypto library
   * @throws JsonProcessingException If Jackson is unable to parse the body to Json for signing
   */
  public ResponseSpec sendInvalidSignatureRequest(String endpoint, Interaction interaction)
      throws DecoderException, JsonProcessingException {
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = createInvalidSignature(interaction, timestamp);
    return this.webTestClient.post().uri("/interactions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Signature-Ed25519", signature)
        .header("X-Signature-Timestamp", timestamp)
        .exchange();
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
}
