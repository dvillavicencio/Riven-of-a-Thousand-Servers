package com.danielvm.destiny2bot.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.discord.DiscordUser;
import com.danielvm.destiny2bot.util.OAuth2Params;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import reactor.test.StepVerifier;

public class RegistrationControllerTest extends BaseIntegrationTest {

  static final String SESSION_COOKIE = "SESSION";
  @Autowired
  BungieConfiguration bungieConfiguration;

  @Autowired
  UserDetailsReactiveDao userDetailsReactiveDao;

  @Test
  @DisplayName("should save Discord user to httpSession after OAuth2 authorization")
  public void discordUserRegistration() throws Exception {

    // given: a login attempt from Discord user is received
    var authorizationCode = "userAuthorizationCode";

    stubFor(post(urlPathEqualTo("/api/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-access-token.json")));

    stubFor(get(urlPathMatching("/api/v10/users/@me"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/user-@me-response.json")));

    stubFor(post(urlPathEqualTo("/platform/app/oauth/token/"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("bungie/bungie-access-token.json")));

    // when: the discord callback request is received
    ResponseSpec discordCallbackResponse = webTestClient.get()
        .uri("/discord/callback?code=" + authorizationCode)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .exchange();

    // then: the response is a redirect URI to Bungie OAuth2 Authorization Flow
    discordCallbackResponse.expectStatus().is3xxRedirection()
        .expectHeader().exists(HttpHeaders.LOCATION)
        .expectHeader().valueEquals(
            HttpHeaders.LOCATION,
            OAuth2Util.bungieAuthorizationUrl(bungieConfiguration.getAuthorizationUrl(),
                bungieConfiguration.getClientId()));

    ResponseCookie sessionCookie = discordCallbackResponse.returnResult(ResponseEntity.class)
        .getResponseCookies()
        .getFirst(SESSION_COOKIE);

    // when: the bungie callback request is received
    ResponseSpec bungieRequest = webTestClient.get()
        .uri("/bungie/callback?code=" + authorizationCode)
        .cookie(SESSION_COOKIE, Objects.requireNonNull(sessionCookie).getValue())
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: bungie call back response returns status 204 NO_CONTENT
    bungieRequest
        .expectStatus().is2xxSuccessful()
        .expectStatus().isNoContent();

    // and: the user is saved in the database
    var discordUserFile = new ClassPathResource("__files/discord/user-@me-response.json");
    var discordUser = objectMapper.readValue(discordUserFile.getFile(), DiscordUser.class);

    StepVerifier.create(userDetailsReactiveDao.existsByDiscordId(discordUser.getId()))
        .assertNext(Boolean.TRUE::equals)
        .verifyComplete();
  }

  @Test
  @DisplayName("user registration fails when Discord returns a 4xx error")
  public void userRegistrationFailsWhenDiscordReturns4xxError() throws Exception {

    // given: a login attempt from Discord user is received
    var authorizationCode = "userAuthorizationCode";
    var discordErrorJson = """
        {
            "message": "401: Unauthorized",
            "code": 0
        }
        """;

    stubFor(post(urlPathEqualTo("/api/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(discordErrorJson)));

    // when: the discord callback request from a login attempt is received
    ResponseSpec response = webTestClient.get()
        .uri("/discord/callback?code=" + authorizationCode)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: the server returns a 4xx BAD_REQUEST status code
    response
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.detail").isEqualTo(discordErrorJson)
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @DisplayName("user registration fails when Discord token call returns a 5xx error")
  public void userRegistrationFailsWhenDiscordReturns5xxError() throws Exception {

    // given: a login attempt from Discord user is received
    var authorizationCode = "userAuthorizationCode";
    var discordErrorJson = """
        {
            "message": "500: Something bad happened",
            "code": 0
        }
        """;

    stubFor(post(urlPathEqualTo("/api/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(discordErrorJson)));

    // when: the discord callback request from a login attempt is received
    ResponseSpec response = webTestClient.get()
        .uri("/discord/callback?code=" + authorizationCode)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: the server returns a 5xx INTERNAL_SEVER status code
    response
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.detail").isEqualTo(discordErrorJson)
        .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Test
  @DisplayName("user registration fails when Bungie returns a 4xx error")
  public void userRegistrationFailsWhenBungieReturns4xxError() throws Exception {

    // given: a login attempt from Discord user is received
    var authorizationCode = "userAuthorizationCode";
    var bungieErrorJson = """
        {
            "ErrorCode": 99,
            "ThrottleSeconds": 0,
            "ErrorStatus": "WebAuthRequired",
            "Message": "Please sign-in to continue.",
            "MessageData": {}
        }""";

    stubFor(post(urlPathEqualTo("/api/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-access-token.json")));

    stubFor(get(urlPathMatching("/api/v10/users/@me"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/user-@me-response.json")));

    stubFor(post(urlPathEqualTo("/platform/app/oauth/token/"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(bungieErrorJson)));

    // when: the discord callback request is received
    ResponseSpec discordCallbackResponse = webTestClient.get()
        .uri("/discord/callback?code=" + authorizationCode)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: the response is a redirect URI to Bungie OAuth2 Authorization Flow
    discordCallbackResponse
        .expectStatus().is3xxRedirection()
        .expectHeader().exists(HttpHeaders.LOCATION)
        .expectHeader().valueEquals(HttpHeaders.LOCATION,
            OAuth2Util.bungieAuthorizationUrl(bungieConfiguration.getAuthorizationUrl(),
                bungieConfiguration.getClientId()));

    ResponseCookie sessionCookie = discordCallbackResponse.returnResult(ResponseEntity.class)
        .getResponseCookies()
        .getFirst(SESSION_COOKIE);

    // when: the bungie callback request is received from a user login
    ResponseSpec bungieCallbackResponse = webTestClient.get()
        .uri("/bungie/callback?code=" + authorizationCode)
        .cookie(SESSION_COOKIE, Objects.requireNonNull(sessionCookie).getValue())
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: bungie call back response returns status a 500 INTERNAL_SERVER error
    bungieCallbackResponse
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.detail").isEqualTo(bungieErrorJson)
        .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @DisplayName("user registration fails when Bungie returns a 5xx error")
  public void userRegistrationFailsWhenBungieReturns5xxError() throws Exception {

    // given: a login attempt from Discord user is received
    var authorizationCode = "userAuthorizationCode";
    var bungieErrorJson = """
        {
            "ErrorCode": 99,
            "ThrottleSeconds": 0,
            "ErrorStatus": "WebAuthRequired",
            "Message": "Please sign-in to continue.",
            "MessageData": {}
        }""";
    var discordRequest = MockMvcRequestBuilders.get("/discord/callback")
        .queryParam(OAuth2Params.CODE, authorizationCode)
        .accept(MediaType.APPLICATION_JSON);

    stubFor(post(urlPathEqualTo("/api/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-access-token.json")));

    stubFor(get(urlPathMatching("/api/v10/users/@me"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/user-@me-response.json")));

    stubFor(post(urlPathEqualTo("/platform/app/oauth/token/"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT,
            containing(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(aResponse()
            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(bungieErrorJson)));

    // when: the discord callback request is received
    ResponseSpec discordCallbackResponse = webTestClient.get()
        .uri("/discord/callback?code=" + authorizationCode)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: the response is a redirect URI to Bungie OAuth2 Authorization Flow
    discordCallbackResponse.expectStatus().is3xxRedirection()
        .expectHeader().exists(HttpHeaders.LOCATION)
        .expectHeader().valueEquals(
            HttpHeaders.LOCATION,
            OAuth2Util.bungieAuthorizationUrl(bungieConfiguration.getAuthorizationUrl(),
                bungieConfiguration.getClientId()));

    ResponseCookie sessionCookie = discordCallbackResponse.returnResult(ResponseEntity.class)
        .getResponseCookies()
        .getFirst(SESSION_COOKIE);

    // when: the bungie callback request is received from a user login
    ResponseSpec bungieCallbackResponse = webTestClient.get()
        .uri("/bungie/callback?code=" + authorizationCode)
        .cookie(SESSION_COOKIE, Objects.requireNonNull(sessionCookie).getValue())
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // then: bungie call back response returns status a 500 INTERNAL_SERVER error
    bungieCallbackResponse
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.detail").isEqualTo(bungieErrorJson)
        .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }
}
