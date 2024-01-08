package com.danielvm.destiny2bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.entity.UserDetails;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationServiceTest {

  private static final TokenResponse GENERIC_TOKEN_RESPONSE = new TokenResponse(
      "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
      "IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
      3600L,
      "Bearer");
  @Mock
  private DiscordConfiguration discordConfigurationMock;
  @Mock
  private BungieConfiguration bungieConfigurationMock;
  @Mock
  private DiscordClient discordClientMock;
  @Mock
  private UserDetailsReactiveDao userDetailsReactiveDaoMock;
  @Spy
  private WebClient.Builder defaultWebClientBuilderMock;
  @Mock
  private RequestBodyUriSpec requestBodyUriSpec;
  @Mock
  private RequestHeadersSpec requestHeadersSpec;
  @Mock
  private ResponseSpec responseSpec;
  @Mock
  private WebClient webClientMock;
  @InjectMocks
  private UserRegistrationService sut;

  static Stream<Arguments> discordUsersWithMissingAttributes() {
    return Stream.of(
        arguments(new DiscordUserResponse(null, "someUsername", "avatar", "en_US")),
        arguments(new DiscordUserResponse("someId", null, "avatar", "en_US")));
  }

  static Stream<Arguments> bungieTokenMissingAttributes() {
    return Stream.of(
        arguments(new TokenResponse(null, "Bearer", 3600L, "someRefreshToken")),
        arguments(new TokenResponse("someAccessToken", "Bearer", null, "someRefreshToken")),
        arguments(new TokenResponse("someAccessToken", "Bearer", 3600L, null)));
  }

  @Test
  @DisplayName("authenticate Discord user is successful")
  public void authenticateDiscordUserIsSuccessful() {
    // given: authorization code and an HttpSession
    var authorizationCode = "someAuthorizationCode";
    var httpSession = new MockHttpSession();

    var discordUserId = "88012312784012";
    var discordUsername = "generic_discord_user";
    var discordAvatar = "generic_avatar";
    var locale = Locale.US;
    DiscordUserResponse discordUser = new DiscordUserResponse(discordUserId, discordUsername,
        discordAvatar, locale.toString());

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(GENERIC_TOKEN_RESPONSE));

    when(discordClientMock.getUser(
        OAuth2Util.formatBearerToken(GENERIC_TOKEN_RESPONSE.getAccessToken())))
        .thenReturn(Mono.just(discordUser));

    // when: authenticateDiscordUser is called
    StepVerifier.create(sut.authenticateDiscordUser(authorizationCode, httpSession))
        .assertNext(
            entity -> {
              var status = entity.getStatusCode();
              var relocationUrl = Objects.requireNonNull(
                  entity.getHeaders().get(HttpHeaders.LOCATION)).get(0);
              Assertions.assertThat(relocationUrl)
                  .isEqualTo("http://discord.auth.url/oauth/auth?response_type=code&client_id");
              Assertions.assertThat(status).isEqualTo(HttpStatus.FOUND);
            })
        .verifyComplete();

    // then: the HttpSession has session attributes related to it
    assertThat(httpSession.getAttribute("discordUserId")).isEqualTo(discordUserId);
    assertThat(httpSession.getAttribute("discordUserAlias")).isEqualTo(discordUsername);
  }

  @Test
  @DisplayName("authenticate Discord fails if access_token is null")
  public void authenticateDiscordUserFailsIfAccessTokenIsNull() {
    // given: authorization code and an HttpSession
    var authorizationCode = "someAuthorizationCode";
    var httpSession = new MockHttpSession();

    TokenResponse nullAccessToken = new TokenResponse(
        null, null, null, null);

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(nullAccessToken));

    // when: authenticateDiscordUser is called, an IllegalArgumentException is thrown
    StepVerifier.create(sut.authenticateDiscordUser(authorizationCode, httpSession))
        .expectErrorMessage(
            "The access token, the refresh token or the expiration are null for user")
        .verify();
  }

  @ParameterizedTest
  @MethodSource("discordUsersWithMissingAttributes")
  @DisplayName("authenticate Discord user fails if user response is missing attributes")
  public void authenticateDiscordUserFailsIfUserIsMissingAttributes(
      DiscordUserResponse userArgument) {
    // given: authorization code and an HttpSession
    var authorizationCode = "someAuthorizationCode";
    var httpSession = new MockHttpSession();

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(GENERIC_TOKEN_RESPONSE));

    when(discordClientMock.getUser(
        OAuth2Util.formatBearerToken(GENERIC_TOKEN_RESPONSE.getAccessToken())))
        .thenReturn(Mono.just(userArgument));

    // when: authenticateDiscordUser is called an IllegalStateException is thrown
    StepVerifier.create(sut.authenticateDiscordUser(authorizationCode, httpSession))
        .expectErrorMessage(
            "Some required arguments for registration are null for the current user")
        .verify();
  }

  @Test
  @DisplayName("authenticate Discord user fails if user response empty")
  public void authenticateDiscordUserFailsIfUserIsEmpty() {
    // given: authorization code and an HttpSession
    var authorizationCode = "someAuthorizationCode";
    var httpSession = new MockHttpSession();

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(GENERIC_TOKEN_RESPONSE));

    when(discordClientMock.getUser(
        OAuth2Util.formatBearerToken(GENERIC_TOKEN_RESPONSE.getAccessToken())))
        .thenReturn(Mono.empty());

    // when: authenticateDiscordUser is called an IllegalStateException is thrown
    StepVerifier.create(sut.authenticateDiscordUser(authorizationCode, httpSession))
        .expectErrorMessage("The user response from Discord is empty")
        .verify();
  }

  @Test
  @DisplayName("registering a new user using Bungie and Discord data is successful")
  public void registerDiscordAndBungieUserIsSuccessful() {
    // given: authorization code and an HttpSession with previous Discord data
    var authorizationCode = "someAuthorizationCode";

    var discordUserId = "88012312784012";
    var discordUsername = "generic_discord_user";

    var httpSession = new MockHttpSession();
    httpSession.setAttribute("discordUserId", discordUserId);
    httpSession.setAttribute("discordUserAlias", discordUsername);

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(GENERIC_TOKEN_RESPONSE));

    UserDetails databaseEntity = UserDetails.builder()
        .discordId(discordUserId)
        .discordUsername(discordUsername)
        .expiration(any())
        .refreshToken(GENERIC_TOKEN_RESPONSE.getRefreshToken())
        .accessToken(GENERIC_TOKEN_RESPONSE.getAccessToken())
        .build();

    lenient().when(userDetailsReactiveDaoMock.save(databaseEntity))
        .thenReturn(Mono.just(Boolean.TRUE));

    // when: linkDiscordUserToBungieAccount is called
    StepVerifier.create(sut.linkDiscordUserToBungieAccount(authorizationCode, httpSession))
        .assertNext(next -> assertThat(httpSession.isInvalid()).isTrue());
  }

  @ParameterizedTest
  @MethodSource("bungieTokenMissingAttributes")
  @DisplayName("Linking and registering user fails if Bungie's access token is missing fields")
  public void linkDiscordUserToBungieFailsIfSomeFieldsAreNotPresent(
      TokenResponse bungieToken) {
    // given: authorization code and an HttpSession with previous Discord data
    var authorizationCode = "someAuthorizationCode";

    var discordUserId = "88012312784012";
    var discordUsername = "generic_discord_user";

    var httpSession = new MockHttpSession();
    httpSession.setAttribute("discordUserId", discordUserId);
    httpSession.setAttribute("discordUserAlias", discordUsername);

    mockGetTokenMethodCalls();

    when(responseSpec.bodyToMono(TokenResponse.class))
        .thenReturn(Mono.just(bungieToken));

    // when: linkDiscordUserToBungieAccount is called, an exception is thrown with an error message
    StepVerifier.create(sut.linkDiscordUserToBungieAccount(authorizationCode, httpSession))
        .expectErrorMessage(
            "The access token, the refresh token or the expiration are null for user")
        .verify();
  }

  public void mockGetTokenMethodCalls() {
    // Leniency is needed for these two mock
    lenient().when(discordConfigurationMock.getAuthorizationUrl())
        .thenReturn("http://discord.auth.url/oauth/auth");

    lenient().when(bungieConfigurationMock.getAuthorizationUrl())
        .thenReturn("http://discord.auth.url/oauth/auth");

    lenient().when(discordConfigurationMock.getTokenUrl())
        .thenReturn("http://discord.token.url/oauth/token");

    lenient().when(bungieConfigurationMock.getTokenUrl())
        .thenReturn("http://bungie.token.url/oauth/token");

    when(defaultWebClientBuilderMock.baseUrl(anyString()))
        .thenReturn(defaultWebClientBuilderMock);

    when(defaultWebClientBuilderMock.defaultHeader(HttpHeaders.CONTENT_TYPE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .thenReturn(defaultWebClientBuilderMock);

    when(defaultWebClientBuilderMock.defaultHeader(HttpHeaders.ACCEPT,
        MediaType.APPLICATION_JSON_VALUE))
        .thenReturn(defaultWebClientBuilderMock);

    when(defaultWebClientBuilderMock.build())
        .thenReturn(webClientMock);

    when(webClientMock.post())
        .thenReturn(requestBodyUriSpec);

    when(requestBodyUriSpec.body(any()))
        .thenReturn(requestHeadersSpec);

    when(requestHeadersSpec.retrieve())
        .thenReturn(responseSpec);
  }

}
