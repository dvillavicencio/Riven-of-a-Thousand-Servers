package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.entity.CharacterRaid;
import com.danielvm.destiny2bot.entity.UserCharacter;
import com.danielvm.destiny2bot.entity.UserCharacterIndexing;
import com.danielvm.destiny2bot.entity.UserIndexing;
import com.danielvm.destiny2bot.enums.DestinyClass;
import com.danielvm.destiny2bot.enums.IndexingStatus;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.repository.CharacterRaidRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserRaidDataService {

  private static final Integer COUNT_MAX = 250;
  private static final Integer RAID_MODE = 4;

  private static final String COMPLETED_KEY = "completed";
  private static final String TIME_PLAYED_KEY = "timePlayedSeconds";
  private static final String KDA_KEY = "killsDeathsAssists";
  private static final String OPPONENTS_KEY = "opponentsDefeated";
  private static final String DEATHS_KEY = "deaths";

  private final BotUserRepository botUserRepository;
  private final BungieAPIService bungieAPIService;
  private final CharacterRaidRepository characterRaidRepository;
  private final IndexingInformationService indexingInformationService;
  private final DiscordStatusMessageService discordStatusMessageService;

  private final BiFunction<Activity, String, Double> getStatIdValue = (activity, key) ->
      activity.getValues().containsKey(key) ? activity.getValues().get(key).getBasic().getValue()
          : null;

  public UserRaidDataService(
      BotUserRepository botUserRepository,
      BungieAPIService bungieAPIService,
      CharacterRaidRepository characterRaidRepository,
      IndexingInformationService indexingInformationService,
      DiscordStatusMessageService indexingStatusService) {
    this.botUserRepository = botUserRepository;
    this.bungieAPIService = bungieAPIService;
    this.characterRaidRepository = characterRaidRepository;
    this.indexingInformationService = indexingInformationService;
    this.discordStatusMessageService = indexingStatusService;
  }

  private static String formatDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;
    return String.format("%02d hour(s) and %02d minute(s)", hours, minutes);
  }

  private static String resolveRaidName(String directorActivityName) {
    String[] tokens = directorActivityName.split(":");
    return tokens[0].trim();
  }

  private static Boolean resolveHardMode(String activityName) {
    String[] tokens = activityName.split(":");
    if (tokens.length == 1) {
      return null;
    }
    String hardModeIdentifier = tokens[1].trim();
    if (hardModeIdentifier.equalsIgnoreCase("Master")) {
      return true;
    } else if (hardModeIdentifier.equalsIgnoreCase("Normal")) {
      return false;
    } else {
      return null;
    }
  }

  /**
   * Load all user details and characters for a Bot User
   *
   * @param botUser The current bot user
   */
  @Transactional
  public void loadUserDetailsAndCharacters(BotUser botUser) {
    List<UserCharacter> characterList = bungieAPIService.getCharacters(
            botUser.getBungieMembershipType(), botUser.getBungieMembershipId())
        .getCharacters().getData().entrySet().stream()
        .map(entry -> UserCharacter.builder()
            .botUser(botUser)
            .lightLevel(entry.getValue().getLight())
            .destinyClass(DestinyClass.findByCode(entry.getValue().getClassType()).getName())
            .characterId(entry.getKey()) // key is the characterId
            .build())
        .toList();
    botUser.setCharacters(characterList);
    botUserRepository.save(botUser);
  }

  /**
   * Load all the raid activity history for a bot user The current raids that are being taken into
   * consideration are:
   * <li>Full raids from the beginning</li>
   * <li>Partial raids that were not started from the beginning but were completed</li>
   *
   * @param user The bot user to index their characters for
   */
  @Transactional
  public void loadCharactersActivityHistory(BotUser user) {
    UserIndexing currentUserInfo = initializeIndexing(user);
    user.getCharacters().forEach(character -> {
      indexCharacters(user, currentUserInfo, character);
    });
    finalizeIndexing(currentUserInfo);
  }

  private void indexCharacters(BotUser user, UserIndexing currentUserInfo,
      UserCharacter character) {
    UserCharacterIndexing characterIndexing = UserCharacterIndexing.builder()
        .userIndexing(currentUserInfo)
        .characterId(character.getCharacterId())
        .build();
    int pageNumber = 0;
    boolean notLastPage = true;
    discordStatusMessageService.updateStatusMessage(character, IndexingStatus.INDEXING);
    while (notLastPage) {
      try {
        notLastPage = indexCharacter(user, character, pageNumber++, characterIndexing);
      } catch (Throwable throwable) {
        notLastPage = false;
        discordStatusMessageService.updateStatusMessage(character, IndexingStatus.ERROR);
      }
    }
    finalizeCharacterIndexing(currentUserInfo, character, characterIndexing);
  }

  private void finalizeCharacterIndexing(UserIndexing currentUserInfo, UserCharacter character,
      UserCharacterIndexing characterIndexing) {
    indexingInformationService.addCharacterInfo(currentUserInfo, characterIndexing);
    discordStatusMessageService.updateStatusMessage(character, IndexingStatus.SUCCESS);
  }

  private void finalizeIndexing(UserIndexing currentUserInfo) {
    discordStatusMessageService.closeStatusMessage();
    indexingInformationService.finalizeIndexing(currentUserInfo);
  }

  private UserIndexing initializeIndexing(BotUser user) {
    UserIndexing currentUserInfo = indexingInformationService.initiateIndexing(
        user.getDiscordId());
    discordStatusMessageService.initializeStatusMessage(user.getDiscordId(),
        user.getDiscordUsername());
    return currentUserInfo;
  }

  private boolean indexCharacter(BotUser user, UserCharacter character,
      Integer pageNumber, UserCharacterIndexing characterIndexing) {
    var activityHistory = bungieAPIService.getCharacterActivityHistory(
        user.getBungieMembershipType(), user.getBungieMembershipId(),
        characterIndexing.getCharacterId(), COUNT_MAX, RAID_MODE, 0);
    if (CollectionUtils.isEmpty(activityHistory.getActivities())) {
      characterIndexing.setLastPage(pageNumber);
      return false;
    }

    List<Activity> activities = activityHistory.getActivities();
    activities.forEach(activity -> {
      boolean isActivityUncompleted =
          activity.getValues().get(COMPLETED_KEY).getBasic().getValue() == 0;
      if (isActivityUncompleted) {
        return;
      }

      Long hashIdentifier = activity.getActivityDetails().getDirectorActivityHash();
      String directorActivity = bungieAPIService.getManifestEntity(
              ManifestEntity.ACTIVITY_DEFINITION, hashIdentifier)
          .getDisplayProperties().getName();

      String raidName = resolveRaidName(directorActivity);
      Boolean isHardMode = resolveHardMode(directorActivity);

      Long raidInstance = activity.getActivityDetails().getInstanceId();
      PostGameCarnageReport report = bungieAPIService.getPGCR(user.getBungieAccessToken(),
          activity.getActivityDetails().getInstanceId());

      Duration raidDuration = Duration.ofSeconds(
          getStatIdValue.apply(activity, TIME_PLAYED_KEY).longValue());

      CharacterRaid raid = CharacterRaid.builder()
          .userCharacterId(character.getCharacterId())
          .raidName(raidName)
          .raidStartTimestamp(activity.getPeriod())
          .instanceId(raidInstance)
          .kda(getStatIdValue.apply(activity, KDA_KEY))
          .completed(true)
          .isFromBeginning(report.getActivityWasStartedFromBeginning())
          .raidDuration(formatDuration(raidDuration))
          .opponentsDefeated(getStatIdValue.apply(activity, OPPONENTS_KEY).intValue())
          .isHardMode(isHardMode)
          .numberOfDeaths(getStatIdValue.apply(activity, DEATHS_KEY).intValue())
          .build();
      characterRaidRepository.save(raid);
    });

    if (activities.size() >= COUNT_MAX) {
      return true;
    } else {
      characterIndexing.setLastPage(pageNumber);
      return false;
    }
  }

}
