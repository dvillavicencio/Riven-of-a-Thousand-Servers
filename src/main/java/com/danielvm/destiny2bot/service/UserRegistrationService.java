package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.util.MembershipUtil;
import com.danielvm.destiny2bot.util.OAuth2Util;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
public class UserRegistrationService {

  private static final String DISCORD_USER_ID_KEY = "discordUserId";
  private static final String DISCORD_USER_ALIAS_KEY = "discordUserAlias";

  private final DiscordConfiguration discordConfiguration;
  private final BungieConfiguration bungieConfiguration;
  private final DiscordClient discordClient;
  private final BungieClient bungieClient;
  private final UserRaidDataService userRaidDataService;

  public UserRegistrationService(
      DiscordConfiguration discordConfiguration,
      BungieConfiguration bungieConfiguration,
      DiscordClient imperativeDiscordClient,
      BungieClient imperativeBungieClient,
      UserRaidDataService userRaidDataService) {
    this.discordConfiguration = discordConfiguration;
    this.bungieConfiguration = bungieConfiguration;
    this.discordClient = imperativeDiscordClient;
    this.bungieClient = imperativeBungieClient;
    this.userRaidDataService = userRaidDataService;
  }

  private static TokenResponse verifyTokenParameters(ResponseEntity<TokenResponse> tokenResponse) {
    if (tokenResponse == null || tokenResponse.getBody() == null ||
        tokenResponse.getBody().getAccessToken() == null ||
        tokenResponse.getBody().getRefreshToken() == null ||
        tokenResponse.getBody().getExpiresIn() == null) {
      log.error("Token response parameters are null from Discord");
      throw new InternalServerException(
          "Required Token response parameters from Discord are not present",
          HttpStatus.BAD_GATEWAY);
    }
    return tokenResponse.getBody();
  }

  private static DiscordUserResponse verifyUserDetails(
      ResponseEntity<DiscordUserResponse> userDetails) {
    if (userDetails == null || userDetails.getBody() == null ||
        userDetails.getBody().getUsername() == null || userDetails.getBody().getId() == null) {
      log.error("Required parameters for a Discord user are null or something went wrong");
      throw new InternalServerException(
          "Required parameters for a Discord user are not valid or not present",
          HttpStatus.BAD_GATEWAY);
    }
    return userDetails.getBody();
  }

  /**
   * Retrieve DiscordUserId from authenticated user and save it to Session
   *
   * @param authorizationCode The authorization code from Discord
   * @param session           The HttpSession the user is linked to
   */
  public void authenticateDiscordUser(String authorizationCode, HttpSession session) {
    MultiValueMap<String, String> tokenExchangeParameters = OAuth2Util.buildTokenExchangeParameters(
        authorizationCode, discordConfiguration.getCallbackUrl(),
        discordConfiguration.getClientSecret(), discordConfiguration.getClientId()
    );
    TokenResponse tokenResponse = verifyTokenParameters(discordClient.getAccessToken(
        tokenExchangeParameters));

    String bearerToken = OAuth2Util.formatBearerToken(tokenResponse.getAccessToken());

    DiscordUserResponse userDetails = verifyUserDetails(discordClient.getUser(bearerToken));

    session.setAttribute(DISCORD_USER_ID_KEY, userDetails.getId());
    session.setAttribute(DISCORD_USER_ALIAS_KEY, userDetails.getUsername());
  }

  /**
   * Save user details for a Discord user and link their Bungie information in the database after
   * OAuth2 bungie authorization
   * <ul>
   * <li>Links user-specific details like bungie membershipId, discordId, and their access and refresh tokens</li>
   * <li>Links Destiny 2 user's characters</li>
   * <li>Loads Raid information for each character</li>
   * </ul>
   *
   * @param authorizationCode The authorization code from Bungie
   * @param httpSession       The HttpSession the user is linked to
   */
  @Async
  public void authenticateBungieUser(String authorizationCode, HttpSession httpSession) {
    MultiValueMap<String, String> tokenExchangeParameters = OAuth2Util.buildTokenExchangeParameters(
        authorizationCode, bungieConfiguration.getCallbackUrl(),
        bungieConfiguration.getClientSecret(), bungieConfiguration.getClientId()
    );
    TokenResponse token = verifyTokenParameters(
        bungieClient.getAccessToken(tokenExchangeParameters));
    Long discordId = (Long) httpSession.getAttribute(DISCORD_USER_ID_KEY);
    String discordUser = (String) httpSession.getAttribute(DISCORD_USER_ALIAS_KEY);

    MembershipResponse membershipInfo = MembershipUtil.verifyMembershipParameters(
        bungieClient.getMembershipForCurrentUser(
            OAuth2Util.formatBearerToken(token.getAccessToken())),
        discordUser).getBody();

    Long membershipId = MembershipUtil.extractMembershipId(membershipInfo);
    Integer membershipType = MembershipUtil.extractMembershipType(membershipInfo);

    BotUser botUser = BotUser.builder()
        .discordId(discordId)
        .discordUsername(discordUser)
        .bungieAccessToken(token.getAccessToken())
        .bungieRefreshToken(token.getRefreshToken())
        .bungieTokenExpiration(token.getExpiresIn())
        .bungieMembershipId(membershipId)
        .bungieMembershipType(membershipType)
        .build();

    log.info("Starting Destiny 2 Character load process for user [{}]", discordUser);
    userRaidDataService.loadUserDetailsAndCharacters(botUser);
    userRaidDataService.loadCharactersActivityHistory(botUser);
    log.info("Finished Destiny 2 Character loading process for user [{}]", discordUser);
  }

}
