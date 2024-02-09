package com.danielvm.destiny2bot.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.destiny.membership.DestinyMembershipData;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.destiny.membership.Memberships;
import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.util.OAuth2Util;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationServiceTest {

  @Mock
  BungieConfiguration bungieConfiguration;
  @Mock
  DiscordClient discordClient;
  @Mock
  BungieClient bungieClient;
  @Mock
  UserRaidDataService userRaidDataService;
  @Mock
  DiscordConfiguration discordConfiguration;

  @InjectMocks
  UserRegistrationService sut;

  static Stream<Arguments> tokenWithoutResponseFields() {
    return Stream.of(
        arguments(new TokenResponse(null, "Bearer", 3600L, "RefreshToken")),
        arguments(new TokenResponse("accessToken", "Bearer", null, "RefreshToken")),
        arguments(new TokenResponse("accessToken", "Bearer", 3600L, null))
    );
  }

  static Stream<Arguments> discordUserResponseWithoutResponseFields() {
    return Stream.of(
        arguments(new DiscordUserResponse(1L, null, null, "en_US")),
        arguments(new DiscordUserResponse(null, "deahtstroke", null, "en_US"))
    );
  }

  static Stream<Arguments> bungieMembershipResponseWithoutResponseFields() {
    return Stream.of(
        arguments(new MembershipResponse(null)),
        arguments(new MembershipResponse(new Memberships(null))),
        arguments(new MembershipResponse(new Memberships(new ArrayList<>()))),
        arguments(new MembershipResponse(new Memberships(List.of(
            new DestinyMembershipData(null, 3L))))),
        arguments(new MembershipResponse(new Memberships(List.of(
            new DestinyMembershipData(3, null))))),
        arguments(new MembershipResponse(new Memberships(List.of(
            new DestinyMembershipData(3, 3L),
            new DestinyMembershipData(3, null)))))
    );
  }

  @Before
  public void before() {
    when(discordConfiguration.getCallbackUrl())
        .thenReturn("https://test.app/discord/callback");
    when(discordConfiguration.getClientSecret())
        .thenReturn("verySecretiveSecret");
    when(discordConfiguration.getClientId())
        .thenReturn("someClientId");

    when(bungieConfiguration.getCallbackUrl())
        .thenReturn("https://test.app/bungie/callback");
    when(bungieConfiguration.getClientSecret())
        .thenReturn("verySecretiveSecret");
    when(bungieConfiguration.getClientId())
        .thenReturn("someClientId");
  }

  @Test
  @DisplayName("Authenticate discord user works successfully")
  public void discordAuthenticationTest() {
    // given: a discord authorization code
    String authCode = "asdmfo0913aasdmGUMpmosa";
    HttpSession mockSession = new MockHttpSession();

    var parameters = OAuth2Util.buildTokenExchangeParameters(authCode,
        discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId());
    TokenResponse tokenResponse = new TokenResponse("access_token", "Bearer", 3600L,
        "refresh_token");
    DiscordUserResponse userResponse = new DiscordUserResponse(1L, "Deahtstroke", null, "en_US");

    when(discordClient.getAccessToken(parameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(discordClient.getUser("Bearer access_token")).thenReturn(ResponseEntity.ok(userResponse));

    // when: authentication with discord happens
    sut.authenticateDiscordUser(authCode, mockSession);

    // then: the mock https session has the correct attributes
    Assertions.assertThat(mockSession.getAttribute("discordUserId")).isEqualTo(1L);
    Assertions.assertThat(mockSession.getAttribute("discordUserAlias")).isEqualTo("Deahtstroke");

    // and: the expected interactions occurred
    verify(discordClient, times(1)).getAccessToken(parameters);
    verify(discordClient, times(1)).getUser("Bearer access_token");
  }

  @Test
  @DisplayName("Should throw the correct exception when token response null")
  public void emptyOrNullTokenResponses() {
    // given: an auth code and an http session
    String authCode = "asdmfo0913aasdmGUMpmosa";
    HttpSession mockSession = new MockHttpSession();

    var parameters = OAuth2Util.buildTokenExchangeParameters(authCode,
        discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId());
    TokenResponse tokenResponse = null;

    when(discordClient.getAccessToken(parameters)).thenReturn(ResponseEntity.ok(tokenResponse));

    // when: authenticating a discord user is called
    // then: the correct exception is thrown with the correct message
    assertThrows(
        InternalServerException.class,
        () -> sut.authenticateDiscordUser(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // and: the correct number of interactions happen
    verify(discordClient, times(1)).getAccessToken(parameters);
  }

  @ParameterizedTest
  @MethodSource("tokenWithoutResponseFields")
  @DisplayName("Should throw the correct exception when required token fields are null")
  public void emptyTokenFields(TokenResponse tokenResponse) {
    // given: an auth code and an http session
    String authCode = "asdmfo0913aasdmGUMpmosa";
    HttpSession mockSession = new MockHttpSession();

    var parameters = OAuth2Util.buildTokenExchangeParameters(authCode,
        discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId());

    when(discordClient.getAccessToken(parameters)).thenReturn(ResponseEntity.ok(tokenResponse));

    // when: authenticating a discord user is called
    // then: the correct exception is thrown with the correct message
    assertThrows(
        InternalServerException.class,
        () -> sut.authenticateDiscordUser(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // and: the correct number of interactions happen
    verify(discordClient, times(1)).getAccessToken(parameters);
  }

  @Test
  @DisplayName("Should throw the correct exception when Discord's user@me API returns a null response")
  public void nullDiscordUserResponse() {
    // given: an auth code and an http session
    String authCode = "asdmfo0913aasdmGUMpmosa";
    HttpSession mockSession = new MockHttpSession();

    var parameters = OAuth2Util.buildTokenExchangeParameters(authCode,
        discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId());
    TokenResponse tokenResponse = new TokenResponse("access_token", "Bearer", 3600L,
        "refresh_token");

    when(discordClient.getAccessToken(parameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(discordClient.getUser("Bearer access_token")).thenReturn(ResponseEntity.ok(null));

    // when: authenticating a discord user is called
    // then: the correct exception is thrown with the correct message
    assertThrows(
        InternalServerException.class,
        () -> sut.authenticateDiscordUser(authCode, mockSession),
        "Required parameters for a Discord user are not valid or not present");

    // and: the correct number of interactions happen
    verify(discordClient, times(1)).getAccessToken(parameters);
    verify(discordClient, times(1)).getUser("Bearer access_token");
  }

  @ParameterizedTest
  @MethodSource("discordUserResponseWithoutResponseFields")
  @DisplayName("Should throw the correct exception when Discord's user@me API returns an empty required parameters")
  public void emptyDiscordUserResponse(DiscordUserResponse userResponse) {
    // given: an auth code and an http session
    String authCode = "asdmfo0913aasdmGUMpmosa";
    HttpSession mockSession = new MockHttpSession();

    var parameters = OAuth2Util.buildTokenExchangeParameters(authCode,
        discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId());
    TokenResponse tokenResponse = new TokenResponse("access_token", "Bearer", 3600L,
        "refresh_token");

    when(discordClient.getAccessToken(parameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(discordClient.getUser("Bearer access_token")).thenReturn(ResponseEntity.ok(userResponse));

    // when: authenticating a discord user is called
    // then: the correct exception is thrown with the correct message
    assertThrows(
        InternalServerException.class,
        () -> sut.authenticateDiscordUser(authCode, mockSession),
        "Required parameters for a Discord user are not valid or not present");

    // and: the correct number of interactions happen
    verify(discordClient, times(1)).getAccessToken(parameters);
    verify(discordClient, times(1)).getUser("Bearer access_token");
  }

  @Test
  @DisplayName("Saving user details is successful")
  public void successfulUserDetailsSaving() {
    // given: an authorization code and a mock http session
    String authCode = "19301lsa1;;1sdfklmaspdf1XC";
    HttpSession mockSession = new MockHttpSession();
    mockSession.setAttribute("discordUserAlias", "Deahtstroke");
    mockSession.setAttribute("discordUserId", 1L);
    var tokenParameters = OAuth2Util.buildTokenExchangeParameters(
        authCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    TokenResponse tokenResponse = new TokenResponse("accessToken", "Bearer", 3600L, "refreshToken");
    Memberships memberships = new Memberships(List.of(
        new DestinyMembershipData(3, 1L)
    ));
    MembershipResponse membershipResponse = new MembershipResponse(memberships);

    when(bungieClient.getAccessToken(tokenParameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(bungieClient.getMembershipForCurrentUser("Bearer accessToken")).thenReturn(
        ResponseEntity.ok(membershipResponse));

    // when: save user details is called
    sut.saveUserDetails(authCode, mockSession);

    // then: the correct interactions happens
    verify(bungieClient, times(1)).getAccessToken(tokenParameters);
    verify(bungieClient, times(1)).getMembershipForCurrentUser("Bearer accessToken");
    verify(userRaidDataService, times(1)).loadUserDetailsAndCharacters(any());
    verify(userRaidDataService, times(1)).loadCharactersActivityHistory(any());
  }

  @Test
  @DisplayName("Saving user details fails if token response is null")
  public void userDetailsFailsTokenNull() {
    // given: an authorization code and a mock http session
    String authCode = "19301lsa1;;1sdfklmaspdf1XC";
    HttpSession mockSession = new MockHttpSession();
    mockSession.setAttribute("discordUserAlias", "Deahtstroke");
    mockSession.setAttribute("discordUserId", 1L);
    var tokenParameters = OAuth2Util.buildTokenExchangeParameters(
        authCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    when(bungieClient.getAccessToken(tokenParameters)).thenReturn(ResponseEntity.ok(null));

    // when: save user details is called
    // then: the appropriate exception is thrown with the correct message
    assertThrows(InternalServerException.class,
        () -> sut.saveUserDetails(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // then: the correct interactions happens
    verify(bungieClient, times(1)).getAccessToken(tokenParameters);
    verify(bungieClient, times(0)).getMembershipForCurrentUser("Bearer accessToken");
    verify(userRaidDataService, times(0)).loadUserDetailsAndCharacters(any());
    verify(userRaidDataService, times(0)).loadCharactersActivityHistory(any());
  }

  @ParameterizedTest
  @MethodSource("tokenWithoutResponseFields")
  @DisplayName("Saving user details fails if token response is missing required attributes")
  public void userDetailsFailsTokenMissingParameters(TokenResponse tokenResponse) {
    // given: an authorization code and a mock http session
    String authCode = "19301lsa1;;1sdfklmaspdf1XC";
    HttpSession mockSession = new MockHttpSession();
    mockSession.setAttribute("discordUserAlias", "Deahtstroke");
    mockSession.setAttribute("discordUserId", 1L);
    var tokenParameters = OAuth2Util.buildTokenExchangeParameters(
        authCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    when(bungieClient.getAccessToken(tokenParameters)).thenReturn(ResponseEntity.ok(tokenResponse));

    // when: save user details is called
    // then: the appropriate exception is thrown with the correct message
    assertThrows(InternalServerException.class,
        () -> sut.saveUserDetails(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // then: the correct interactions happens
    verify(bungieClient, times(1)).getAccessToken(tokenParameters);
    verify(bungieClient, times(0)).getMembershipForCurrentUser("Bearer accessToken");
    verify(userRaidDataService, times(0)).loadUserDetailsAndCharacters(any());
    verify(userRaidDataService, times(0)).loadCharactersActivityHistory(any());
  }

  @Test
  @DisplayName("Saving user details fails if bungie membership response is null")
  public void saveUserDetailsFailsNullMembershipAttributes() {
    // given: an authorization code and a mock http session
    String authCode = "19301lsa1;;1sdfklmaspdf1XC";
    HttpSession mockSession = new MockHttpSession();
    mockSession.setAttribute("discordUserAlias", "Deahtstroke");
    mockSession.setAttribute("discordUserId", 1L);
    var tokenParameters = OAuth2Util.buildTokenExchangeParameters(
        authCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    TokenResponse tokenResponse = new TokenResponse("accessToken", "Bearer", 3600L, "refreshToken");

    when(bungieClient.getAccessToken(tokenParameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(bungieClient.getMembershipForCurrentUser("Bearer accessToken")).thenReturn(
        ResponseEntity.ok(null));

    // when: save user details is called
    // then: the appropriate exception is thrown with the correct message
    assertThrows(InternalServerException.class,
        () -> sut.saveUserDetails(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // then: the correct interactions happens
    verify(bungieClient, times(1)).getAccessToken(tokenParameters);
    verify(bungieClient, times(1)).getMembershipForCurrentUser("Bearer accessToken");
    verify(userRaidDataService, times(0)).loadUserDetailsAndCharacters(any());
    verify(userRaidDataService, times(0)).loadCharactersActivityHistory(any());
  }

  @ParameterizedTest
  @MethodSource("bungieMembershipResponseWithoutResponseFields")
  @DisplayName("Saving user details fails if bungie membership response is null")
  public void saveUserDetailsFailsMissingMembershipAttributes(
      MembershipResponse membershipResponse) {
    // given: an authorization code and a mock http session
    String authCode = "19301lsa1;;1sdfklmaspdf1XC";
    HttpSession mockSession = new MockHttpSession();
    mockSession.setAttribute("discordUserAlias", "Deahtstroke");
    mockSession.setAttribute("discordUserId", 1L);
    var tokenParameters = OAuth2Util.buildTokenExchangeParameters(
        authCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    TokenResponse tokenResponse = new TokenResponse("accessToken", "Bearer", 3600L, "refreshToken");

    when(bungieClient.getAccessToken(tokenParameters)).thenReturn(ResponseEntity.ok(tokenResponse));
    when(bungieClient.getMembershipForCurrentUser("Bearer accessToken")).thenReturn(
        ResponseEntity.ok(membershipResponse));

    // when: save user details is called
    // then: the appropriate exception is thrown with the correct message
    assertThrows(InternalServerException.class,
        () -> sut.saveUserDetails(authCode, mockSession),
        "Token response parameters from Discord were returned as null");

    // then: the correct interactions happens
    verify(bungieClient, times(1)).getAccessToken(tokenParameters);
    verify(bungieClient, times(1)).getMembershipForCurrentUser("Bearer accessToken");
    verify(userRaidDataService, times(0)).loadUserDetailsAndCharacters(any(BotUser.class));
    verify(userRaidDataService, times(0)).loadCharactersActivityHistory(any());
  }

}
