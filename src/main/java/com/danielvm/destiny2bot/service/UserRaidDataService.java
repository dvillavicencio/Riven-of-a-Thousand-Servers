package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.BungieManifestClient;
import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ManifestFields;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.entity.CharacterRaid;
import com.danielvm.destiny2bot.entity.UserCharacter;
import com.danielvm.destiny2bot.enums.DestinyClass;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.model.state.ErrorState;
import com.danielvm.destiny2bot.model.state.IndexingState;
import com.danielvm.destiny2bot.model.state.MutableMessagePart;
import com.danielvm.destiny2bot.model.state.SuccessState;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.repository.CharacterRaidRepository;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
public class UserRaidDataService {

  private static final Integer COUNT_MAX = 250;
  private static final Integer RAID_MODE = 4;

  private final BotUserRepository botUserRepository;
  private final BungieClient bungieClient;
  private final BungieManifestClient bungieManifestClient;
  private final BungieClient pgcrBungieClient;
  private final CharacterRaidRepository characterRaidRepository;
  private final IndexingStatusService indexingStatusService;

  private final BiFunction<Activity, String, Double> getStatIdValue = (activity, key) -> activity.getValues()
      .get(key).getBasic().getValue();

  public UserRaidDataService(
      BotUserRepository botUserRepository,
      BungieClient imperativeBungieClient,
      BungieManifestClient bungieManifestClient,
      BungieClient pgcrBungieClient,
      CharacterRaidRepository characterRaidRepository,
      IndexingStatusService indexingStatusService) {
    this.botUserRepository = botUserRepository;
    this.bungieClient = imperativeBungieClient;
    this.bungieManifestClient = bungieManifestClient;
    this.pgcrBungieClient = pgcrBungieClient;
    this.characterRaidRepository = characterRaidRepository;
    this.indexingStatusService = indexingStatusService;
  }

  private static String formatDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;
    return String.format("%02d hour(s) and %02d minute(s)", hours, minutes);
  }

  /**
   * Load all user details and characters for a Bot User
   *
   * @param membershipId   The user's Bungie MembershipId
   * @param membershipType The user's Bungie MembershipType
   * @param botUser        The current bot user
   */
  public void loadUserDetailsAndCharacters(Long membershipId, Integer membershipType,
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

  /**
   * Load all the raid activity history for a bot user
   * The current raids that are being taken into consideration are:
   * <li>Full raids from the beginning</li>
   * <li>Partial raids that were not started from the beginning but were completed</li>
   *
   * @param user The bot user to index their characters for
   */
  public void loadCharactersActivityHistory(BotUser user) {

    indexingStatusService.prepareIndexingStatusMessage(user.getDiscordId(),
        user.getDiscordUsername());

    user.getCharacters().forEach(character -> {
      // Here we notify that we started indexing for this character;
      MutableMessagePart mutableMessagePart = new MutableMessagePart();
      String characterContent = "%s - Light Level %s".formatted(character.getDestinyClass(),
          character.getLightLevel());
      mutableMessagePart.setStatefulContent(characterContent);

      IndexingState.updateState(mutableMessagePart);
      indexingStatusService.updateState(character.getCharacterId(), mutableMessagePart);
      IndexingState.logStatus(user.getDiscordId(), characterContent, character.getCharacterId());

      Integer pageNumber = 0;
      boolean notLastPage = true;
      while (notLastPage) {
        try {
          notLastPage = indexCharacter(user, character, pageNumber);
          pageNumber++;
        } catch (Throwable throwable) {
          ErrorState.updateState(mutableMessagePart);
          indexingStatusService.updateState(character.getCharacterId(), mutableMessagePart);
          ErrorState.logStatus(user.getDiscordId(), characterContent, character.getCharacterId());
        }
      }
      // Here we notify that we finished indexing for this character
      SuccessState.updateState(mutableMessagePart);
      indexingStatusService.updateState(character.getCharacterId(), mutableMessagePart);
      SuccessState.logStatus(user.getDiscordId(), characterContent, character.getCharacterId());
    });
    indexingStatusService.finishIndexing();
  }

  private boolean indexCharacter(BotUser user, UserCharacter character,
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
      String raidName = retrieveRaidName(activity);
      boolean isActivityComplete =
          activity.getValues().get("completed").getBasic().getValue() == 0;

      // Ignoring uncompleted activities for now
      if (isActivityComplete) {
        return;
      }

      Long raidInstance = activity.getActivityDetails().getInstanceId();
      PostGameCarnageReport report = retrievePGCR(user, raidInstance);

      Duration raidDuration = Duration.ofSeconds(
          getStatIdValue.apply(activity, "timePlayedSeconds").longValue());

      CharacterRaid raid = CharacterRaid.builder()
          .userCharacterId(character.getCharacterId())
          .raidName(raidName)
          .raidStartTimestamp(activity.getPeriod())
          .instanceId(raidInstance)
          .kda(getStatIdValue.apply(activity, "killsDeathsAssists"))
          .completed(true)
          .isFromBeginning(report.getActivityWasStartedFromBeginning())
          .raidDuration(formatDuration(raidDuration))
          .opponentsDefeated(getStatIdValue.apply(activity, "opponentsDefeated").intValue())
          .numberOfDeaths(getStatIdValue.apply(activity, "deaths").intValue())
          .build();

      characterRaidRepository.save(raid);
    });
    return activities.size() >= COUNT_MAX;
  }

  private PostGameCarnageReport retrievePGCR(BotUser user, Long raidInstance) {
    BungieResponse<PostGameCarnageReport> report = pgcrBungieClient.getPostCarnageReport(
        OAuth2Util.formatBearerToken(user.getBungieAccessToken()), raidInstance).getBody();
    if (Objects.isNull(report) || Objects.isNull(report.getResponse())) {
      String errorMessage = "A PGCR for a given raid did not include some required details. User [%s], Raid instance [%s]"
          .formatted(user, raidInstance);
      log.error(errorMessage);
      throw new InternalServerException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return report.getResponse();
  }

  private String retrieveRaidName(Activity activity) {
    BungieResponse<ManifestFields> response = bungieManifestClient.getManifestEntity(
            ManifestEntity.ACTIVITY_DEFINITION, activity.getActivityDetails().getDirectorActivityHash())
        .getBody();

    if (Objects.isNull(response) || Objects.isNull(response.getResponse()) || Objects.isNull(
        response.getResponse().getDisplayProperties()) || Objects.isNull(
        response.getResponse().getDisplayProperties().getName())) {
      String errorMessage = "The manifest entity for activity with hash [%s] returned a null for a required field".formatted(
          activity.getActivityDetails().getDirectorActivityHash());
      log.error(errorMessage);
      throw new InternalServerException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response.getResponse().getDisplayProperties().getName();
  }

}
