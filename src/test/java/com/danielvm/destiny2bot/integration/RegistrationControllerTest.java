package com.danielvm.destiny2bot.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.util.OAuth2Util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

public class RegistrationControllerTest extends BaseIntegrationTest {

  @Autowired
  BungieConfiguration bungieConfiguration;

  @Test
  @DisplayName("Initial Discord OAuth2 callback works as expected")
  public void discordCallback() throws Exception {
    // given: an authorization code from Discord
    String authorizationCode = "180840kao0klal-xkqw";
    var request = MockMvcRequestBuilders.get("/discord/callback")
        .queryParam("code", authorizationCode);

    stubFor(post(urlPathEqualTo("/discord/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(containing("code=" + authorizationCode))
        .withRequestBody(containing("grant_type=authorization_code"))
        .withRequestBody(containing("client_secret=someClientSecret"))
        .withRequestBody(containing("client_id=someClientId"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-access-token.json")));

    stubFor(get(urlPathEqualTo("/discord/users/@me"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer someAccessToken"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/user-@me-response.json")));

    // when: the discord OAuth2 callback endpoint is called
    var response = mockMvc.perform(request);

    // then: a 302 FOUND response is returned with the Bungie OAuth2 authorization link
    response.andDo(MockMvcResultHandlers.print())
        .andExpect(status().is3xxRedirection())
        .andExpect(status().is(302))
        .andExpect(header().string(HttpHeaders.LOCATION,
            OAuth2Util.bungieAuthorizationUrl(
                bungieConfiguration.getAuthorizationUrl(),
                bungieConfiguration.getClientId())));
  }

  @Test
  @DisplayName("Initial Discord OAuth2 callback fails if a Token response field is missing")
  public void discordCallbackMissingTokenField() throws Exception {
    // given: an authorization code from Discord
    String authorizationCode = "180840kao0klal-xkqw";
    var request = MockMvcRequestBuilders.get("/discord/callback")
        .queryParam("code", authorizationCode);

    stubFor(post(urlPathEqualTo("/discord/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(containing("code=" + authorizationCode))
        .withRequestBody(containing("grant_type=authorization_code"))
        .withRequestBody(containing("client_secret=someClientSecret"))
        .withRequestBody(containing("client_id=someClientId"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-token-missing.json")));

    // when: the discord OAuth2 callback endpoint is called
    var response = mockMvc.perform(request);

    // then: a BAD_GATEWAY response with the appropriate error message
    response.andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value(
            "Required Token response parameters from Discord are not present"))
        .andExpect(jsonPath("$.status").value(502));
  }

  @Test
  @DisplayName("Discord OAuth2 callback fails if Discord user API is missing required attributes")
  public void discordCallbackMissingUserProperties() throws Exception {
    // given: an authorization code from Discord
    String authorizationCode = "180840kao0klal-xkqw";
    var request = MockMvcRequestBuilders.get("/discord/callback")
        .queryParam("code", authorizationCode);

    stubFor(post(urlPathEqualTo("/discord/oauth2/token"))
        .withHeader(HttpHeaders.CONTENT_TYPE,
            containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(containing("code=" + authorizationCode))
        .withRequestBody(containing("grant_type=authorization_code"))
        .withRequestBody(containing("client_secret=someClientSecret"))
        .withRequestBody(containing("client_id=someClientId"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/discord-access-token.json")));

    stubFor(get(urlPathEqualTo("/discord/users/@me"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer someAccessToken"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("discord/user-@me-missing.json")));

    // when: the discord OAuth2 callback endpoint is called
    var response = mockMvc.perform(request);

    // then: a BAD_GATEWAY status response is returned with the appropriate error message
    response.andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value(
            "Required parameters for a Discord user are not valid or not present"))
        .andExpect(jsonPath("$.status").value(502));
  }

  @Test
  @DisplayName("authenticating bungie user is successful")
  public void authenticatingBungieUserSuccess() {

  }
}
