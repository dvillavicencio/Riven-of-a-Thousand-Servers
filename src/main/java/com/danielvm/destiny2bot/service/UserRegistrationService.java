package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.BungieManifestClient;
import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.config.OAuth2Configuration;
import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.entity.CharacterRaid;
import com.danielvm.destiny2bot.entity.UserCharacter;
import com.danielvm.destiny2bot.enums.DestinyClass;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.repository.CharacterRaidRepository;
import com.danielvm.destiny2bot.util.MembershipUtil;
import com.danielvm.destiny2bot.util.OAuth2Util;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

@Service
@Slf4j
public class UserRegistrationService {

  private static final String DISCORD_USER_ID_KEY = "discordUserId";
  private static final String DISCORD_USER_ALIAS_KEY = "discordUserAlias";
  private static final Integer COUNT_MAX = 250;
  private static final Integer RAID_MODE = 4;

  private final DiscordConfiguration discordConfiguration;
  private final BungieConfiguration bungieConfiguration;
  private final DiscordClient discordClient;
  private final BungieClient bungieClient;
  private final BungieClient pgcrBungieClient;
  private final RestClient.Builder defaultRestClientBuilder;
  private final BotUserRepository botUserRepository;
  private final BungieManifestClient bungieManifestClient;
  private final CharacterRaidRepository characterRaidRepository;

  public UserRegistrationService(
      DiscordConfiguration discordConfiguration,
      BungieConfiguration bungieConfiguration,
      DiscordClient imperativeDiscordClient,
      BungieClient imperativeBungieClient,
      BungieClient pgcrBungieClient,
      Builder defaultRestClientBuilder,
      BotUserRepository botUserRepository,
      BungieManifestClient bungieManifestClient,
      CharacterRaidRepository characterRaidRepository) {
    this.discordConfiguration = discordConfiguration;
    this.bungieConfiguration = bungieConfiguration;
    this.discordClient = imperativeDiscordClient;
    this.bungieClient = imperativeBungieClient;
    this.defaultRestClientBuilder = defaultRestClientBuilder;
    this.botUserRepository = botUserRepository;
    this.bungieManifestClient = bungieManifestClient;
    this.characterRaidRepository = characterRaidRepository;
    this.pgcrBungieClient = pgcrBungieClient;
  }

  private static String formatDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;
    return String.format("%02d hour(s) and %02d minute(s)", hours, minutes);
  }

  /**
   * Retrieve DiscordUserId from authenticated user and save it to Session
   *
   * @param authorizationCode The authorization code from Discord
   * @param session           The HttpSession the user is linked to
   */
  public void authenticateDiscordUser(String authorizationCode, HttpSession session) {
    TokenResponse tokenResponse = getTokenResponse(authorizationCode,
        discordConfiguration).getBody();

    String bearerToken = tokenResponse.getAccessToken();

    ResponseEntity<DiscordUserResponse> userDetails = discordClient.getUser(
        OAuth2Util.formatBearerToken(bearerToken));

    session.setAttribute(DISCORD_USER_ID_KEY, userDetails.getBody().getId());
    session.setAttribute(DISCORD_USER_ALIAS_KEY, userDetails.getBody().getUsername());
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
  public void saveUserDetails(String authorizationCode,
      HttpSession httpSession) {
    TokenResponse token = getTokenResponse(authorizationCode, bungieConfiguration).getBody();
    Long discordId = (Long) httpSession.getAttribute(DISCORD_USER_ID_KEY);
    String discordUser = (String) httpSession.getAttribute(DISCORD_USER_ALIAS_KEY);

    MembershipResponse membershipInfo = bungieClient.getMembershipForCurrentUser(
        OAuth2Util.formatBearerToken(token.getAccessToken())).getBody();

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
    loadUserDetailsAndCharacters(membershipId, membershipType, botUser);
    loadCharactersActivityHistory(botUser);
    log.info("Finished Destiny 2 Character loading process for user [{}]", discordUser);
  }

  private void loadCharactersActivityHistory(BotUser user) {
    user.getCharacters().forEach(character -> {
      Integer pageNumber = 0;
      boolean notLastPage = true;
      while (notLastPage) {
        notLastPage = indexActivitiesForCharacter(user, character, pageNumber);
        pageNumber++;
      }
    });
  }

  private boolean indexActivitiesForCharacter(BotUser user, UserCharacter character,
      Integer pageNumber) {
    var response = bungieClient.getCharacterActivityHistory(user.getBungieMembershipType(),
        user.getBungieMembershipId(), character.getCharacterId(),
        COUNT_MAX, RAID_MODE, pageNumber).getBody();

    if (Objects.isNull(response) || Objects.isNull(response.getResponse())
        || CollectionUtils.isEmpty(response.getResponse().getActivities())) {
      return false;
    }
    List<Activity> activities = response.getResponse().getActivities();

    activities.forEach(activity -> {
      String raidName = bungieManifestClient.getManifestEntity(ManifestEntity.ACTIVITY_DEFINITION,
              activity.getActivityDetails().getDirectorActivityHash()).getBody().getResponse()
          .getDisplayProperties().getName();
      boolean isActivityComplete =
          activity.getValues().get("completed").getBasic().getValue() == 0;

      // Ignoring uncompleted activities for now
      if (isActivityComplete) {
        return;
      }

      Long raidInstance = activity.getActivityDetails().getInstanceId();
      PostGameCarnageReport report = pgcrBungieClient.getPostCarnageReport(
              OAuth2Util.formatBearerToken(user.getBungieAccessToken()), raidInstance).getBody()
          .getResponse();

      Duration raidDuration = Duration.ofSeconds(activity.getValues().get("timePlayedSeconds")
          .getBasic().getValue().longValue());

      CharacterRaid raid = CharacterRaid.builder()
          .userCharacterId(character.getCharacterId())
          .raidName(raidName)
          .raidStartTimestamp(activity.getPeriod())
          .instanceId(raidInstance)
          .kda(activity.getValues().get("killsDeathsAssists").getBasic().getValue())
          .completed(true)
          .isFromBeginning(report.getActivityWasStartedFromBeginning())
          .raidDuration(formatDuration(raidDuration))
          .opponentsDefeated(
              activity.getValues().get("opponentsDefeated").getBasic().getValue().intValue())
          .numberOfDeaths(activity.getValues().get("deaths").getBasic().getValue().intValue())
          .build();

//      List<RaidParticipant> participants = report.getEntries().stream()
//          .filter(player -> {
//            boolean playerCompletedRaid = player.getValues().get("completed").getBasic()
//                .getDisplayValue()
//                .equalsIgnoreCase("Yes");
//            boolean isPublicProfile = player.getPlayer().getDestinyUserInfo().getIsPublic();
//            return playerCompletedRaid && isPublicProfile;
//          })
//          .map(entry -> {
//            DestinyUserInfo playerInfo = entry.getPlayer().getDestinyUserInfo();
//            String userTag = playerInfo.getBungieGlobalDisplayName() + "#"
//                             + playerInfo.getBungieGlobalDisplayNameCode();
//            String iconPath = entry.getPlayer().getDestinyUserInfo().getIconPath();
//            return new RaidParticipant(user.getBungieMembershipId(), userTag, entry.getPlayer()
//                .getCharacterClass(), iconPath, true, null);
//          }).toList();
//
//      raid.setParticipants(participants);
      characterRaidRepository.save(raid);
    });
    return activities.size() >= COUNT_MAX;
  }

  private void loadUserDetailsAndCharacters(Long membershipId, Integer membershipType,
      BotUser botUser) {
    BungieResponse<CharactersResponse> characters = bungieClient.getUserCharacters(
        membershipType, membershipId).getBody();

    List<UserCharacter> characterList = characters.getResponse().getCharacters().getData()
        .entrySet().stream()
        .map(entry -> UserCharacter.builder()
            .botUser(botUser)
            .lightLevel(entry.getValue().getLight())
            .destinyClass(DestinyClass.findByCode(entry.getValue().getClassType()).getName())
            .characterId(Long.valueOf(entry.getKey()))
            .build())
        .toList();
    botUser.setCharacters(characterList);
    botUserRepository.save(botUser);
  }

  private ResponseEntity<TokenResponse> getTokenResponse(String authorizationCode,
      OAuth2Configuration oAuth2Configuration) {
    MultiValueMap<String, String> map =
        OAuth2Util.buildTokenExchangeParameters(authorizationCode,
            oAuth2Configuration.getCallbackUrl(), oAuth2Configuration.getClientSecret(),
            oAuth2Configuration.getClientId());

    var tokenClient = defaultRestClientBuilder
        .baseUrl(oAuth2Configuration.getTokenUrl())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();

    return tokenClient.post()
        .body(map)
        .retrieve()
        .toEntity(TokenResponse.class);
  }

}
