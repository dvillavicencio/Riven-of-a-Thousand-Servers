package com.danielvm.destiny2bot.service;

import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.BungieClientWrapper;
import com.danielvm.destiny2bot.dto.WeeklyActivity;
import com.danielvm.destiny2bot.dto.destiny.GenericResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.DisplayProperties;
import com.danielvm.destiny2bot.dto.destiny.manifest.ResponseFields;
import com.danielvm.destiny2bot.dto.destiny.milestone.ActivitiesDto;
import com.danielvm.destiny2bot.dto.destiny.milestone.MilestoneEntry;
import com.danielvm.destiny2bot.enums.ActivityMode;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class WeeklyActivitiesServiceTest {

  @Mock
  private BungieClient bungieClient;
  @Mock
  private BungieClientWrapper bungieClientWrapper;
  @InjectMocks
  private WeeklyActivitiesService sut;

  @Test
  @DisplayName("Retrieve weekly raid works successfully")
  public void retrieveWeeklyRaidWorksSuccessfully() {
    // given: an activity mode
    ActivityMode activity = ActivityMode.RAID;

    var startTime = ZonedDateTime.now();
    var endTime = ZonedDateTime.now().plusDays(2L);
    var activitiesNoWeekly = List.of(new ActivitiesDto("1262462921", Collections.emptyList()));
    var activitiesWeekly = List.of(new ActivitiesDto("2823159265", List.of("897950155")));
    var milestoneResponse = new GenericResponse<>(
        Map.of(
            "526718853", new MilestoneEntry("526718853",
                startTime, endTime, activitiesNoWeekly),

            "3618845105", new MilestoneEntry("3618845105",
                startTime, endTime, activitiesWeekly),

            "2029743966", new MilestoneEntry("2029743966",
                startTime, endTime, null)
        )
    );

    when(bungieClient.getPublicMilestonesRx())
        .thenReturn(Mono.just(milestoneResponse));

    var activityWithType = new ResponseFields();
    activityWithType.setActivityTypeHash("608898761");
    var raidActivityEntity = new GenericResponse<>(activityWithType);

    when(
        bungieClientWrapper.getManifestEntityRx(ManifestEntity.ACTIVITY_DEFINITION, "2823159265"))
        .thenReturn(Mono.just(raidActivityEntity));

    var raidResponseFields = new ResponseFields();
    raidResponseFields.setDisplayProperties(
        new DisplayProperties("someDescription", "Raid", null, null, false));
    var activityTypeEntity = new GenericResponse<>(raidResponseFields);

    when(bungieClientWrapper.getManifestEntityRx(ManifestEntity.ACTIVITY_TYPE_DEFINITION,
        "608898761"))
        .thenReturn(Mono.just(activityTypeEntity));

    var milestoneResponseFields = new ResponseFields();
    var lastWishDisplayProperties = new DisplayProperties("Delve into the Last Wish raid",
        "The Last Wish", null, null, false);
    milestoneResponseFields.setDisplayProperties(lastWishDisplayProperties);
    var milestoneEntity = new GenericResponse<>(milestoneResponseFields);

    when(
        bungieClientWrapper.getManifestEntityRx(ManifestEntity.MILESTONE_DEFINITION, "3618845105"))
        .thenReturn(Mono.just(milestoneEntity));

    WeeklyActivity expectedResponse = new WeeklyActivity("The Last Wish",
        "Delve into the Last Wish raid", startTime, endTime);

    // when: retrieve weekly activity is called
    Mono<WeeklyActivity> response = sut.getWeeklyActivity(activity);

    // then: the response matches the expected value
    StepVerifier.create(response)
        .expectNext(expectedResponse)
        .verifyComplete();
  }

  @Test
  @DisplayName("Retrieve weekly dungeon works successfully")
  public void retrieveWeeklyDungeonIsSuccessful() {
    // given: an activity mode
    ActivityMode activity = ActivityMode.DUNGEON;

    var startTime = ZonedDateTime.now();
    var endTime = ZonedDateTime.now().plusDays(2L);
    var activitiesNoWeekly = List.of(new ActivitiesDto("1262462921", Collections.emptyList()));
    var activitiesWeekly = List.of(new ActivitiesDto("2823159265", List.of("897950155")));
    var milestoneResponse = new GenericResponse<>(
        Map.of(
            "526718853", new MilestoneEntry("526718853",
                startTime, endTime, activitiesNoWeekly),

            "3618845105", new MilestoneEntry("3618845105",
                startTime, endTime, activitiesWeekly),

            "2029743966", new MilestoneEntry("2029743966",
                startTime, endTime, null)
        )
    );

    when(bungieClient.getPublicMilestonesRx())
        .thenReturn(Mono.just(milestoneResponse));

    var activityWithType = new ResponseFields();
    activityWithType.setActivityTypeHash("608898761");
    var raidActivityEntity = new GenericResponse<>(activityWithType);

    when(bungieClientWrapper.getManifestEntityRx(ManifestEntity.ACTIVITY_DEFINITION, "2823159265"))
        .thenReturn(Mono.just(raidActivityEntity));

    var dungeonResponseFields = new ResponseFields();
    dungeonResponseFields.setDisplayProperties(
        new DisplayProperties("someDescription", "Dungeon", null, null, false));
    var activityTypeEntity = new GenericResponse<>(dungeonResponseFields);

    when(bungieClientWrapper.getManifestEntityRx(ManifestEntity.ACTIVITY_TYPE_DEFINITION,
        "608898761"))
        .thenReturn(Mono.just(activityTypeEntity));

    var milestoneResponseFields = new ResponseFields();
    var dualityDisplayProperties = new DisplayProperties("Calus' mind as a dungeon lol",
        "Duality", null, null, false);
    milestoneResponseFields.setDisplayProperties(dualityDisplayProperties);
    var milestoneEntity = new GenericResponse<>(milestoneResponseFields);

    when(bungieClientWrapper.getManifestEntityRx(ManifestEntity.MILESTONE_DEFINITION, "3618845105"))
        .thenReturn(Mono.just(milestoneEntity));

    WeeklyActivity expectedResponse = new WeeklyActivity(dualityDisplayProperties.getName(),
        dualityDisplayProperties.getDescription(), startTime, endTime);

    // when: retrieve weekly activity is called
    Mono<WeeklyActivity> response = sut.getWeeklyActivity(activity);

    // then: the response matches the expected value
    StepVerifier.create(response)
        .expectNext(expectedResponse)
        .verifyComplete();
  }

  @Test
  @DisplayName("Get weekly activity should throw an error if the criteria is not met")
  public void getWeeklyActivityError() {
    // given: an activity mode
    ActivityMode activity = ActivityMode.DUNGEON;

    var startTime = ZonedDateTime.now();
    var endTime = ZonedDateTime.now().plusDays(2L);
    var activitiesNoWeekly = List.of(new ActivitiesDto("1262462921", Collections.emptyList()));
    var milestoneResponse = new GenericResponse<>(
        Map.of(
            "526718853", new MilestoneEntry("526718853",
                startTime, endTime, activitiesNoWeekly),

            "2029743966", new MilestoneEntry("2029743966",
                startTime, endTime, null)
        )
    );

    when(bungieClient.getPublicMilestonesRx())
        .thenReturn(Mono.just(milestoneResponse));

    // when: getWeeklyActivity is called
    var stepVerifier = StepVerifier.create(sut.getWeeklyActivity(activity));

    // then: a ResourceNotFoundException is thrown and the error message is correct
    stepVerifier.verifyErrorMessage(
        "No weekly activity found for activity type [%s]".formatted(activity));

    stepVerifier.expectError(ResourceNotFoundException.class).verify();
  }
}
