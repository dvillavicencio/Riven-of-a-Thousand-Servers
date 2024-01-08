package com.danielvm.destiny2bot.service;

import static com.danielvm.destiny2bot.enums.ManifestEntity.ACTIVITY_DEFINITION;
import static com.danielvm.destiny2bot.enums.ManifestEntity.ACTIVITY_TYPE_DEFINITION;
import static com.danielvm.destiny2bot.enums.ManifestEntity.MILESTONE_DEFINITION;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.BungieClientWrapper;
import com.danielvm.destiny2bot.dto.MilestoneResponse;
import com.danielvm.destiny2bot.dto.WeeklyActivity;
import com.danielvm.destiny2bot.dto.destiny.GenericResponse;
import com.danielvm.destiny2bot.dto.destiny.milestone.ActivitiesDto;
import com.danielvm.destiny2bot.dto.destiny.milestone.MilestoneEntry;
import com.danielvm.destiny2bot.enums.ActivityMode;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WeeklyActivitiesService {

  private final BungieClient bungieClient;
  private final BungieClientWrapper bungieClientWrapper;

  public WeeklyActivitiesService(
      BungieClient bungieClient,
      BungieClientWrapper bungieClientWrapper) {
    this.bungieClient = bungieClient;
    this.bungieClientWrapper = bungieClientWrapper;
  }

  /**
   * Fetches a weekly activity based on the activity mode
   *
   * @param activityMode The type of the weekly activity (see {@link ActivityMode})
   * @return {@link MilestoneResponse}
   */
  public Mono<WeeklyActivity> getWeeklyActivity(ActivityMode activityMode) {
    return bungieClient.getPublicMilestonesRx()
        .map(GenericResponse::getResponse)
        .flatMapIterable(Map::values)
        .filter(this::hasWeeklyObjectives)
        .filterWhen(milestoneEntry -> activityModeMatches(milestoneEntry, activityMode))
        .flatMap(this::createWeeklyActivity)
        .next() // ideally there should only be one weekly activity
        .switchIfEmpty(Mono.error(
            new ResourceNotFoundException("No weekly activity found for activity type [%s]"
                .formatted(activityMode))));
  }

  private Mono<WeeklyActivity> createWeeklyActivity(MilestoneEntry milestoneEntry) {
    return bungieClientWrapper.getManifestEntityRx(MILESTONE_DEFINITION,
            milestoneEntry.getMilestoneHash())
        .map(milestoneEntity -> milestoneEntity.getResponse().getDisplayProperties())
        .map(displayProperties ->
            WeeklyActivity.builder()
                .name(displayProperties.getName())
                .description(displayProperties.getDescription())
                .startDate(milestoneEntry.getStartDate())
                .endDate(milestoneEntry.getEndDate())
                .build());
  }

  private Mono<Boolean> activityModeMatches(MilestoneEntry entry, ActivityMode activityMode) {
    if (CollectionUtils.isEmpty(entry.getActivities())) {
      return Mono.just(false);
    }
    return Flux.fromIterable(entry.getActivities())
        .flatMap(activity -> bungieClientWrapper.getManifestEntityRx(
            ACTIVITY_DEFINITION, activity.getActivityHash()))
        .filter(activityDefinition -> Objects.nonNull(
            activityDefinition.getResponse().getActivityTypeHash()))
        .flatMap(activityDefinition -> bungieClientWrapper.getManifestEntityRx(
            ACTIVITY_TYPE_DEFINITION, activityDefinition.getResponse().getActivityTypeHash()))
        .filter(activityTypeDefinition -> Objects.nonNull(
            activityTypeDefinition.getResponse()) && Objects.nonNull(
            activityTypeDefinition.getResponse().getDisplayProperties()))
        .any(activityTypeDefinition -> activityTypeDefinition.getResponse().getDisplayProperties()
            .getName().equalsIgnoreCase(activityMode.getLabel()));
  }

  private boolean hasWeeklyObjectives(MilestoneEntry entry) {
    if (CollectionUtils.isEmpty(entry.getActivities())) {
      return false;
    }
    return entry.getActivities().stream()
        .map(ActivitiesDto::getChallengeObjectiveHashes)
        .flatMap(List::stream)
        .findAny().isPresent();
  }
}
