package com.danielvm.destiny2bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.ActivityBasicInfo;
import com.danielvm.destiny2bot.dto.destiny.ActivityDetails;
import com.danielvm.destiny2bot.dto.destiny.ActivityHistoryResponse;
import com.danielvm.destiny2bot.dto.destiny.ActivityValue;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.dto.destiny.characters.CharacterInfo;
import com.danielvm.destiny2bot.dto.destiny.characters.Characters;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.DisplayProperties;
import com.danielvm.destiny2bot.dto.destiny.manifest.ManifestFields;
import com.danielvm.destiny2bot.entity.BotUser;
import com.danielvm.destiny2bot.entity.UserCharacter;
import com.danielvm.destiny2bot.entity.UserIndexing;
import com.danielvm.destiny2bot.enums.IndexingStatus;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.repository.CharacterRaidRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserRaidDataServiceTest {

  @Mock
  private BotUserRepository botUserRepository;
  @Mock
  private BungieAPIService bungieAPIService;
  @Mock
  private IndexingInformationService indexingInformationService;
  @Mock
  private CharacterRaidRepository characterRaidRepository;
  @Mock
  private DiscordStatusMessageService discordStatusMessageService;

  @InjectMocks
  private UserRaidDataService sut;

  @Test
  @DisplayName("Load user and characters details is successful")
  public void loadUserDetailsAndCharactersIsSuccessful() {
    // given: membershipId, membershipType, and a BotUser entity
    Integer membershipType = 3;
    Long membershipId = 558128390580123L;
    BotUser botUser = BotUser.builder()
        .discordId(1L)
        .bungieMembershipId(membershipId)
        .bungieMembershipType(membershipType)
        .bungieTokenExpiration(3600L)
        .bungieRefreshToken("refresh_token")
        .bungieAccessToken("access_token")
        .build();

    Map<Long, CharacterInfo> characterData = Map.of(
        1L, new CharacterInfo(558128390580123L, 3, 1L, 1810, 0, 3, 0),
        2L, new CharacterInfo(558128390580123L, 3, 2L, 1810, 0, 3, 1),
        3L, new CharacterInfo(558128390580123L, 3, 3L, 1810, 0, 3, 2)
    );

    Characters characters = new Characters(characterData);
    CharactersResponse charactersResponse = new CharactersResponse(characters);

    when(bungieAPIService.getCharacters(membershipType, membershipId))
        .thenReturn(charactersResponse);

    // when: the loading happens
    sut.loadUserDetailsAndCharacters(botUser);

    // then: the correct interactions occurred and the saved entity has the correct fields
    verify(bungieAPIService, times(1)).getCharacters(membershipType, membershipId);
    verify(botUserRepository, times(1))
        .save(assertArg(arg -> {
          assertThat(arg).isInstanceOf(BotUser.class);
          assertThat(arg.getBungieAccessToken()).isEqualTo(botUser.getBungieAccessToken());
          assertThat(arg.getBungieRefreshToken()).isEqualTo(botUser.getBungieRefreshToken());
          assertThat(arg.getDiscordUsername()).isEqualTo(botUser.getDiscordUsername());
          assertThat(arg.getDiscordId()).isEqualTo(botUser.getDiscordId());
          assertThat(arg.getBungieTokenExpiration()).isEqualTo(botUser.getBungieTokenExpiration());
          assertThat(arg.getCharacters().size()).isEqualTo(3);

          UserCharacter titan = arg.getCharacters().stream()
              .filter(u -> u.getCharacterId() == 1L)
              .findFirst().orElse(null);

          assertThat(titan.getDestinyClass()).isEqualTo("Titan");
          assertThat(titan.getLightLevel()).isEqualTo(1810);

          UserCharacter hunter = arg.getCharacters().stream()
              .filter(u -> u.getCharacterId() == 2L)
              .findFirst().orElse(null);

          assertThat(hunter.getDestinyClass()).isEqualTo("Hunter");
          assertThat(hunter.getLightLevel()).isEqualTo(1810);

          UserCharacter warlock = arg.getCharacters().stream()
              .filter(u -> u.getCharacterId() == 3L)
              .findFirst().orElse(null);

          assertThat(warlock.getDestinyClass()).isEqualTo("Warlock");
          assertThat(warlock.getLightLevel()).isEqualTo(1810);
        }));
  }

  @Test
  @DisplayName("Load characters activity history is successful for one character")
  public void loadingActivityHistoryIsSuccessful() {
    // given: a Bot user
    Long discordId = 1L;
    BotUser botUser = BotUser.builder()
        .discordId(discordId)
        .discordUsername("deahtstroke")
        .bungieMembershipId(1L)
        .bungieMembershipType(3)
        .bungieTokenExpiration(3600L)
        .bungieRefreshToken("refresh_token")
        .bungieAccessToken("access_token")
        .build();

    List<UserCharacter> characters = List.of(
        new UserCharacter(discordId, 1810, "Titan", botUser)
    );
    botUser.setCharacters(characters);

    List<Activity> activities = List.of(
        new Activity(Instant.now(),
            new ActivityDetails(12345L, 67890L, false, 3, 1234567890L),
            Map.of(
                "completed",
                new ActivityValue("completed", new ActivityBasicInfo(1.0, "Yes")),
                "timePlayedSeconds",
                new ActivityValue("timePlayedSeconds", new ActivityBasicInfo(3600.0, "1hr 00mins")),
                "killsDeathAssists",
                new ActivityValue("killsDeathAssists", new ActivityBasicInfo(37.11231, "37.1")),
                "opponentsDefeated",
                new ActivityValue("opponentsDefeated", new ActivityBasicInfo(123.0, "123")),
                "deaths",
                new ActivityValue("deaths", new ActivityBasicInfo(7.0, "7"))
            )
        )
    );
    ActivityHistoryResponse activityHistoryResponse = new ActivityHistoryResponse(activities);
    when(bungieAPIService.getCharacterActivityHistory(3, 1L, 1L, 250, 4, 0))
        .thenReturn(activityHistoryResponse);

    ManifestFields manifestFields = ManifestFields.builder()
        .displayProperties(new DisplayProperties("", "Last Wish: Level 55", "", "", true))
        .build();
    when(bungieAPIService.getManifestEntity(ManifestEntity.ACTIVITY_DEFINITION, 1234567890L))
        .thenReturn(manifestFields);

    PostGameCarnageReport report = new PostGameCarnageReport(Instant.now(), true, null);
    when(bungieAPIService.getPGCR("Bearer access_token", 67890L))
        .thenReturn(report);

    UserIndexing userIndexing = new UserIndexing(discordId, true, new ArrayList<>());
    when(indexingInformationService.initiateIndexing(discordId))
        .thenReturn(userIndexing);

    // when: Load activity history for user characters is called
    sut.loadCharactersActivityHistory(botUser);

    // then: verify all external dependencies' calls
    verify(discordStatusMessageService, times(1))
        .initializeStatusMessage(discordId, "deahtstroke");

    verify(indexingInformationService, times(1))
        .initiateIndexing(discordId);

    verify(discordStatusMessageService, times(1)).
        updateStatusMessage(characters.get(0), IndexingStatus.INDEXING);

    verify(bungieAPIService, times(1))
        .getCharacterActivityHistory(3, 1L, 1L, 250, 4, 0);

    verify(bungieAPIService, times(1))
        .getPGCR("access_token", 67890L);

    verify(bungieAPIService, times(1))
        .getManifestEntity(any(), any());

    verify(indexingInformationService, times(1))
        .addCharacterInfo(eq(userIndexing), assertArg(uci -> {
              assertThat(uci.getCharacterId()).isEqualTo(1L);
              assertThat(uci.getLastPage()).isEqualTo(0);
              assertThat(uci.getUserIndexing().getDiscordUserId()).isEqualTo(discordId);
            })
        );

    verify(discordStatusMessageService, times(1)).updateStatusMessage(
        characters.get(0), IndexingStatus.SUCCESS);

    verify(discordStatusMessageService, times(1))
        .closeStatusMessage();

    verify(indexingInformationService, times(1))
        .finalizeIndexing(userIndexing);
  }

  @Test
  @DisplayName("Load characters activity history ignores uncompleted activities")
  public void loadingHistoryActivitySelectivity() {
    // given: a Bot user
    Long discordId = 1L;
    BotUser botUser = BotUser.builder()
        .discordId(discordId)
        .discordUsername("deahtstroke")
        .bungieMembershipId(1L)
        .bungieMembershipType(3)
        .bungieTokenExpiration(3600L)
        .bungieRefreshToken("refresh_token")
        .bungieAccessToken("access_token")
        .build();

    List<UserCharacter> characters = List.of(
        new UserCharacter(discordId, 1810, "Titan", botUser)
    );
    botUser.setCharacters(characters);

    List<Activity> activities = List.of(
        new Activity(Instant.now(),
            new ActivityDetails(12345L, 67890L, false, 3, 1234567890L),
            Map.of(
                "completed",
                new ActivityValue("completed", new ActivityBasicInfo(1.0, "Yes")),
                "timePlayedSeconds",
                new ActivityValue("timePlayedSeconds", new ActivityBasicInfo(3600.0, "1hr 00mins")),
                "killsDeathsAssists",
                new ActivityValue("killsDeathsAssists", new ActivityBasicInfo(37.11231, "37.1")),
                "opponentsDefeated",
                new ActivityValue("opponentsDefeated", new ActivityBasicInfo(123.0, "123")),
                "deaths",
                new ActivityValue("deaths", new ActivityBasicInfo(7.0, "7"))
            )
        ),
        new Activity(Instant.now(), // Activity with a "completed" attribute of 0.0/No
            new ActivityDetails(67890L, 12345L, false, 3, 1234567890L),
            Map.of(
                "completed",
                new ActivityValue("completed", new ActivityBasicInfo(0.0, "No")),
                "timePlayedSeconds",
                new ActivityValue("timePlayedSeconds", new ActivityBasicInfo(3600.0, "1hr 00mins")),
                "killsDeathsAssists",
                new ActivityValue("killsDeathsAssists", new ActivityBasicInfo(37.11231, "37.1")),
                "opponentsDefeated",
                new ActivityValue("opponentsDefeated", new ActivityBasicInfo(123.0, "123")),
                "deaths",
                new ActivityValue("deaths", new ActivityBasicInfo(7.0, "7"))
            )
        )
    );
    ActivityHistoryResponse activityHistoryResponse = new ActivityHistoryResponse(activities);
    when(bungieAPIService.getCharacterActivityHistory(3, 1L, 1L, 250, 4, 0))
        .thenReturn(activityHistoryResponse);

    ManifestFields manifestFields = ManifestFields.builder()
        .displayProperties(new DisplayProperties("", "Last Wish: Level 55", "", "", true))
        .build();
    when(bungieAPIService.getManifestEntity(ManifestEntity.ACTIVITY_DEFINITION, 1234567890L))
        .thenReturn(manifestFields);

    PostGameCarnageReport report = new PostGameCarnageReport(Instant.now(), true, null);
    when(bungieAPIService.getPGCR("access_token", 67890L))
        .thenReturn(report);

    UserIndexing userIndexing = new UserIndexing(discordId, true, new ArrayList<>());
    when(indexingInformationService.initiateIndexing(discordId))
        .thenReturn(userIndexing);

    // when: Load activity history for user characters is called
    sut.loadCharactersActivityHistory(botUser);

    // then: verify there's only calls for one raid instance and there's only one saved raid for the character
    verify(discordStatusMessageService, times(1))
        .initializeStatusMessage(discordId, "deahtstroke");

    verify(indexingInformationService, times(1))
        .initiateIndexing(discordId);

    verify(discordStatusMessageService, times(1)).
        updateStatusMessage(characters.get(0), IndexingStatus.INDEXING);

    verify(bungieAPIService, times(1))
        .getCharacterActivityHistory(3, 1L, 1L, 250, 4, 0);

    verify(bungieAPIService, times(1))
        .getPGCR("access_token", 67890L);

    verify(bungieAPIService, times(1))
        .getManifestEntity(any(), any());

    verify(indexingInformationService, times(1))
        .addCharacterInfo(eq(userIndexing), assertArg(uci -> {
              assertThat(uci.getCharacterId()).isEqualTo(1L);
              assertThat(uci.getLastPage()).isEqualTo(0);
              assertThat(uci.getUserIndexing().getDiscordUserId()).isEqualTo(discordId);
            })
        );

    verify(discordStatusMessageService, times(1)).updateStatusMessage(
        characters.get(0), IndexingStatus.SUCCESS);

    verify(discordStatusMessageService, times(1))
        .closeStatusMessage();

    verify(indexingInformationService, times(1))
        .finalizeIndexing(userIndexing);

    verify(characterRaidRepository, times(1)).save(assertArg(entity -> {
      assertThat(entity.getCompleted()).isTrue();
      assertThat(entity.getIsFromBeginning()).isTrue();
      assertThat(entity.getKda()).isEqualTo(37.11231);
      assertThat(entity.getRaidDuration()).isEqualTo("01 hour(s) and 00 minute(s)");
      assertThat(entity.getInstanceId()).isEqualTo(67890L);
      assertThat(entity.getRaidName()).isEqualTo("Last Wish");
      assertThat(entity.getIsHardMode()).isNull();
      assertThat(entity.getOpponentsDefeated()).isEqualTo(123);
      assertThat(entity.getUserCharacterId()).isEqualTo(1L);
    }));
  }

}
