package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.dto.destiny.ActivitiesResponse;
import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.Basic;
import com.danielvm.destiny2bot.dto.destiny.ValueEntry;
import com.danielvm.destiny2bot.entity.UserDetails;
import com.danielvm.destiny2bot.entity.UserRaidDetails;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.enums.RaidDifficulty;
import com.danielvm.destiny2bot.repository.UserDetailsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserRaidDetailsService {

  private static final Integer MAX_SANE_AMOUNT_OF_RAID_PAGES = 50;
  private static final Integer MAX_PAGE_COUNT = 250;
  private static final String EMPTY_RAID_NAME = "empty_name";

  private static final Integer MAX_MANIFEST_CONCURRENT_CALLS = 2;
  private static final Integer MAX_CONCURRENT_ACTIVITY_HISTORY_CALLS = 3;

  private final UserDetailsRepository userDetailsRepository;
  private final PostGameCarnageService postGameCarnageService;
  private final BungieAPIService bungieAPIService;

  public UserRaidDetailsService(
      UserDetailsRepository userDetailsRepository,
      PostGameCarnageService postGameCarnageService,
      BungieAPIService bungieAPIService) {
    this.userDetailsRepository = userDetailsRepository;
    this.postGameCarnageService = postGameCarnageService;
    this.bungieAPIService = bungieAPIService;
  }

  /**
   * Whether a user exists in the database with the given ID
   *
   * @param userId the user ID to find
   * @return True if it exists, else False
   */
  public Mono<Boolean> existsById(String userId) {
    return userDetailsRepository.existsById(userId);
  }

  /**
   * Creates user details for a new user that hasn't been seen yet
   *
   * @param creationInstant The instant this creation action was fired
   * @param membershipId    The membershipId of the newly created user
   * @param membershipType  The membershipType of the newly created user
   * @param uniqueUsername  The unique username of the newly created user
   * @return {@link UserDetails} that were created and subsequently saved
   */
  public Mono<UserDetails> createUserDetails(Instant creationInstant, String uniqueUsername,
      String membershipId, Integer membershipType) {
    return bungieAPIService.getUserCharacters(membershipType, membershipId)
        .flatMapMany(characters -> Flux.fromIterable(characters.keySet()))
        .flatMap(characterId -> getActivitiesAll(membershipType, membershipId, characterId))
        .flatMap(this::buildRaidDetails, MAX_MANIFEST_CONCURRENT_CALLS)
        .flatMap(this::addPGCRDetails)
        .collectList()
        .flatMap(raidDetails -> {
          UserDetails newEntry = new UserDetails(uniqueUsername, null, creationInstant,
              raidDetails);
          return userDetailsRepository.save(newEntry);
        });
  }

  /**
   * Updates user details for a user that has already been seen by the bot
   *
   * @param updateTimestamp The timestamp when this update action was fired
   * @param membershipId    The membershipId of the newly created user
   * @param membershipType  The membershipType of the updated user
   * @param uniqueUsername  The unique username of the updated user
   * @return {@link UserDetails} that were updated
   */
  public Mono<UserDetails> updateUserDetails(Instant updateTimestamp, String uniqueUsername,
      String membershipId, Integer membershipType) {
    return userDetailsRepository.findById(uniqueUsername)
        .flatMap(userDetails -> bungieAPIService.getUserCharacters(membershipType, membershipId)
            .flatMapIterable(Map::keySet)
            .flatMap(characterId -> getActivitiesUntil(membershipType, membershipId,
                characterId, userDetails.getLastRequestDateTime())
                .flatMap(this::buildRaidDetails, MAX_MANIFEST_CONCURRENT_CALLS)
                .flatMap(this::addPGCRDetails))
            .collectList()
            .flatMap(raidDetails -> {
              userDetails.setLastRequestDateTime(updateTimestamp);
              if (CollectionUtils.isEmpty(raidDetails)) {
                log.warn(
                    "No new raid encounters were found for user [{}]. Last time requested set to: [{}]",
                    uniqueUsername, updateTimestamp);
              } else {
                log.info(
                    "Adding [{}] new raid encounters for user [{}]. Last time requested set to: [{}]",
                    raidDetails.size(), uniqueUsername, updateTimestamp);
                userDetails.getUserRaidDetails().addAll(raidDetails);
              }
              return userDetailsRepository.save(userDetails);
            }));
  }

  /**
   * Get activities from the user's character activity history. This is an exhaustive operation and
   * won't finish until all activities are retrieved
   *
   * @param membershipType The membershipType of the user
   * @param membershipId   The membershipId of the user
   * @param characterId    The characterId of the user
   * @return Flux of all existing activities for the user's character
   */
  public Flux<Activity> getActivitiesAll(Integer membershipType, String membershipId,
      String characterId) {
    return Flux.range(0, MAX_SANE_AMOUNT_OF_RAID_PAGES)
        .flatMapSequential(pageNumber ->
            bungieAPIService.getRaidActivities(membershipType, membershipId,
                characterId, pageNumber), MAX_CONCURRENT_ACTIVITY_HISTORY_CALLS)
        .takeUntil(response ->
            CollectionUtils.isEmpty(response.getActivities()) ||
            response.getActivities().size() < MAX_PAGE_COUNT)
        .filter(response -> Objects.nonNull(response.getActivities()))
        .flatMapIterable(ActivitiesResponse::getActivities)
        .switchIfEmpty(Flux.empty());
  }

  /**
   * Gets all the activities from the user's character activity history until an instant in time.
   * Like {@link #getActivitiesAll} this operation is exhaustive and won't stop until the predicate
   * is true.
   *
   * @param membershipType The membershipType of the user
   * @param membershipId   The membershipId of the user
   * @param characterId    The characterId of the user
   * @param until          The last timestamp when this user was searched for
   * @return Flux of all existing activities
   */
  public Flux<Activity> getActivitiesUntil(Integer membershipType, String membershipId,
      String characterId, Instant until) {
    return Flux.range(0, MAX_SANE_AMOUNT_OF_RAID_PAGES)
        .flatMapSequential(pageNumber ->
            bungieAPIService.getRaidActivities(membershipType, membershipId, characterId,
                pageNumber), MAX_CONCURRENT_ACTIVITY_HISTORY_CALLS)
        .filter(response -> Objects.nonNull(response) && Objects.nonNull(response.getActivities()))
        .takeUntil(response -> {
          boolean nullResponse = Objects.isNull(response);
          boolean emptyActivities = CollectionUtils.isEmpty(response.getActivities());

          List<Activity> activities = response.getActivities();
          long newRaidCount = activities.stream()
              .filter(activity -> activity.getPeriod().isAfter(until))
              .count();
          boolean noMoreNewDataAvailable = newRaidCount < MAX_PAGE_COUNT;
          return nullResponse || emptyActivities || noMoreNewDataAvailable;
        })
        .flatMapIterable(ActivitiesResponse::getActivities)
        .filter(activity -> activity.getPeriod().isAfter(until))
        .switchIfEmpty(Flux.empty());
  }

  private Mono<UserRaidDetails> addPGCRDetails(UserRaidDetails userRaidDetails) {
    return postGameCarnageService.retrievePGCR(userRaidDetails.getInstanceId())
        .map(report -> {
          userRaidDetails.setFromBeginning(report.getFromBeginning());
          return userRaidDetails;
        });
  }

  private Mono<UserRaidDetails> buildRaidDetails(Activity activity) {
    return bungieAPIService.getManifestEntity(ManifestEntity.ACTIVITY_DEFINITION,
            activity.getActivityDetails().getDirectorActivityHash())
        .map(entity -> {
              boolean emptyRaidDetails =
                  entity.getDisplayProperties() == null ||
                  entity.getDisplayProperties().getName() == null;

              String raidName = emptyRaidDetails ? EMPTY_RAID_NAME :
                  resolveRaidName(entity.getDisplayProperties().getName());
              RaidDifficulty raidDifficulty = emptyRaidDetails ? null :
                  resolveRaidDifficult(entity.getDisplayProperties().getName());

              var valuesMap = activity.getValues();
              var instanceId = activity.getActivityDetails().getInstanceId();
              Function<String, Double> retrieveByKey = key -> valuesMap.getOrDefault(
                  key, new ValueEntry(null, new Basic(0.0, "0.0"))).getBasic().getValue();
              return UserRaidDetails.builder()
                  .raidName(raidName)
                  .instanceId(instanceId)
                  .raidDifficulty(raidDifficulty)
                  .totalDeaths(retrieveByKey.apply("deaths").intValue())
                  .totalKills(retrieveByKey.apply("kills").intValue())
                  .kda(retrieveByKey.apply("killsDeathsAssists"))
                  .durationSeconds(retrieveByKey.apply("activityDurationSeconds").intValue())
                  .isCompleted(retrieveByKey.apply("completed") != 0)
                  .build();
            }
        );
  }

  private RaidDifficulty resolveRaidDifficult(String raidName) {
    String[] tokens = raidName.split(":");
    if (tokens.length > 1) {
      return switch (tokens[1].trim()) {
        case "Normal", "normal" -> RaidDifficulty.NORMAL;
        case "Master", "master" -> RaidDifficulty.MASTER;
        default -> null;
      };
    }
    return null;
  }

  private String resolveRaidName(String name) {
    String[] tokens = name.split(":");
    return tokens[0].trim();
  }
}
