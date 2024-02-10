package com.danielvm.destiny2bot.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.dto.destiny.Activity;
import com.danielvm.destiny2bot.dto.destiny.ActivityHistoryResponse;
import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.dto.destiny.characters.CharacterInfo;
import com.danielvm.destiny2bot.dto.destiny.characters.Characters;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ManifestFields;
import com.danielvm.destiny2bot.dto.destiny.membership.DestinyMembershipData;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.destiny.membership.Memberships;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.exception.ExternalServiceException;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class BungieAPIServiceTest {

  @Mock
  private BungieClient bungieClient;
  @InjectMocks
  private BungieAPIService sut;

  static Stream<Arguments> emptyResponses() {
    return Stream.of(
        Arguments.arguments(new BungieResponse<>()),
        Arguments.arguments((Object) null));
  }

  static Stream<Arguments> emptyCharacters() {
    return Stream.of(
        Arguments.arguments(new BungieResponse<>(new CharactersResponse(new Characters(null)))),
        Arguments.arguments(new BungieResponse<>(new CharactersResponse(null))),
        Arguments.arguments(new BungieResponse<>()),
        Arguments.arguments((Object) null));
  }

  @Test
  @DisplayName("Get membership for user should work as expected")
  public void testGetCurrentMembershipInfo() {
    // given
    var bearerToken = "SomeBearerToken";
    var membershipResponse = new MembershipResponse(
        new Memberships(List.of(new DestinyMembershipData(3, 1L))));

    when(bungieClient.getMembershipForCurrentUser(bearerToken))
        .thenReturn(ResponseEntity.ok(membershipResponse));

    // when
    var response = sut.getCurrentUserMembershipInformation(bearerToken);

    // then
    assertAll("Destiny Membership response is correct",
        () -> assertEquals(response.response().destinyMemberships().get(0).membershipId(),
            response.response().destinyMemberships().get(0).membershipId()),
        () -> assertEquals(response.response().destinyMemberships().get(0).membershipType(),
            response.response().destinyMemberships().get(0).membershipType()));
  }

  @Test
  @DisplayName("Get membership for user should fail if membership response data is null")
  public void getCurrentMembershipNullData() {
    // given
    var bearerToken = "SomeBearerToken";

    when(bungieClient.getMembershipForCurrentUser(bearerToken))
        .thenReturn(ResponseEntity.ok(null));

    // when
    assertThrows(IllegalArgumentException.class,
        () -> sut.getCurrentUserMembershipInformation(bearerToken),
        "The membership characters for the current user is null");
  }

  @Test
  @DisplayName("Get membership for user should fail if membershipId is null")
  public void membershipIdNegativeTest() {
    // given
    var bearerToken = "SomeBearerToken";

    var membershipResponse = new MembershipResponse(
        new Memberships(List.of(new DestinyMembershipData(3, null))));
    when(bungieClient.getMembershipForCurrentUser(bearerToken))
        .thenReturn(ResponseEntity.ok(membershipResponse));

    // when
    assertThrows(IllegalArgumentException.class,
        () -> sut.getCurrentUserMembershipInformation(bearerToken),
        "Membership Id is null for current user");
  }

  @Test
  @DisplayName("Get membership for user should fail if membershipType is null")
  public void membershipTypeNegativeTest() {
    // given
    var bearerToken = "SomeBearerToken";
    var membershipResponse = new MembershipResponse(
        new Memberships(List.of(new DestinyMembershipData(null, 1L))));

    when(bungieClient.getMembershipForCurrentUser(bearerToken))
        .thenReturn(ResponseEntity.ok(membershipResponse));

    // when
    assertThrows(IllegalArgumentException.class,
        () -> sut.getCurrentUserMembershipInformation(bearerToken),
        "Membership Type is null for current user");
  }

  @Test
  @DisplayName("Getting manifest entity works correctly")
  public void manifestEntityPositiveTest() {
    // given
    ManifestEntity entity = ManifestEntity.ACTIVITY_DEFINITION;
    Long hash = 82931090412L;

    ManifestFields fields = ManifestFields.builder()
        .hash(hash)
        .build();
    BungieResponse<ManifestFields> response = new BungieResponse<>(fields);
    when(bungieClient.getManifestEntity(entity.getId(), hash))
        .thenReturn(ResponseEntity.ok(response));

    // when
    var manifestResponse = sut.getManifestEntity(entity, hash);

    // then
    Assertions.assertThat(manifestResponse).isNotNull();
    Assertions.assertThat(manifestResponse.getHash()).isEqualTo(hash);

    // and
    verify(bungieClient, times(1)).getManifestEntity(any(), any());
  }

  @ParameterizedTest
  @MethodSource("emptyResponses")
  @DisplayName("Getting manifest entity works correctly")
  public void manifestEntityFailsIfResponseIsNull(BungieResponse<ManifestFields> bungieResponse) {
    // given
    ManifestEntity entity = ManifestEntity.ACTIVITY_DEFINITION;
    Long hash = 82931090412L;

    when(bungieClient.getManifestEntity(entity.getId(), hash))
        .thenReturn(ResponseEntity.ok(bungieResponse));

    // when
    // then
    assertThrows(ExternalServiceException.class, () -> sut.getManifestEntity(entity, hash),
        "The manifest entity for activity of type [%s] with hash [%s] returned a null for a required field"
            .formatted(entity, hash));

    // and
    verify(bungieClient, times(1)).getManifestEntity(any(), any());
  }

  @Test
  @DisplayName("Get Post Game Carnage Report is successful")
  public void getPGCRPositiveTest() {
    // given
    String accessToken = "access_token";
    Long raidInstanceId = 128958382L;
    Instant literallyNow = Instant.now();

    PostGameCarnageReport report = new PostGameCarnageReport(literallyNow, true, null);
    BungieResponse<PostGameCarnageReport> response = new BungieResponse<>(report);
    when(bungieClient.getPostCarnageReport(OAuth2Util.formatBearerToken(accessToken),
        raidInstanceId)).thenReturn(ResponseEntity.ok(response));

    // when
    PostGameCarnageReport pgcrResponse = sut.getPGCR(accessToken, raidInstanceId);

    // then
    Assertions.assertThat(pgcrResponse.getPeriod()).isEqualTo(literallyNow);
    Assertions.assertThat(pgcrResponse.getActivityWasStartedFromBeginning()).isEqualTo(true);

    // and
    verify(bungieClient, times(1)).getPostCarnageReport(any(), any());
  }

  @ParameterizedTest
  @MethodSource("emptyResponses")
  @DisplayName("Get Post Game Carnage Report fails on empty responses")
  public void getPGCRNegativeTest(BungieResponse<PostGameCarnageReport> response) {
    // given
    String accessToken = "access_token";
    Long raidInstanceId = 128958382L;

    when(bungieClient.getPostCarnageReport(OAuth2Util.formatBearerToken(accessToken),
        raidInstanceId)).thenReturn(ResponseEntity.ok(response));

    // when
    // then
    assertThrows(ExternalServiceException.class, () -> sut.getPGCR(accessToken, raidInstanceId),
        "A PGCR for a given raid did not include some required details. Raid instance [%s]"
            .formatted(raidInstanceId));

    // and
    verify(bungieClient, times(1)).getPostCarnageReport(any(), any());
  }

  @Test
  @DisplayName("Getting characters works successfully")
  public void getCharactersSuccessfulTest() {
    // given
    Integer membershipType = 3;
    Long membershipId = 8009581213L;

    Map<Long, CharacterInfo> data = Map.of(
        1L, new CharacterInfo(membershipId, membershipType, 1L, 1810, 1, 1, 1)
    );
    Characters characters = new Characters(data);
    CharactersResponse charactersResponse = new CharactersResponse(characters);
    BungieResponse<CharactersResponse> response = new BungieResponse<>(charactersResponse);

    when(bungieClient.getUserCharacters(membershipType, membershipId))
        .thenReturn(ResponseEntity.ok(response));

    // when
    sut.getCharacters(membershipType, membershipId);

    // then
    verify(bungieClient, times(1)).getUserCharacters(membershipType, membershipId);
  }

  @ParameterizedTest
  @MethodSource("emptyCharacters")
  @DisplayName("Getting characters fails for empty or invalid responses")
  public void getCharactersNegativeTest(BungieResponse<CharactersResponse> response) {
    // given
    Integer membershipType = 3;
    Long membershipId = 8009581213L;

    when(bungieClient.getUserCharacters(membershipType, membershipId))
        .thenReturn(ResponseEntity.ok(response));

    // when
    // then
    assertThrows(ExternalServiceException.class,
        () -> sut.getCharacters(membershipType, membershipId),
        "The character response for user with membership type [%s] and membershipId [%s] is missing required fields"
            .formatted(membershipType, membershipId));

    // and
    verify(bungieClient, times(1)).getUserCharacters(membershipType, membershipId);
  }

  @Test
  @DisplayName("Getting activity history for a character is successful")
  public void activityHistoryPositiveTest() {
    // given
    Integer membershipType = 3;
    Long membershipId = 1895002813L;
    Long characterId = 5782930231L;
    Integer count = 250;
    Integer mode = 4;
    Integer pageNumber = 0;

    List<Activity> activities = new ArrayList<>();
    ActivityHistoryResponse activityHistoryResponse = new ActivityHistoryResponse(activities);
    BungieResponse<ActivityHistoryResponse> response = new BungieResponse<>(
        activityHistoryResponse);

    when(bungieClient.getCharacterActivityHistory(membershipType, membershipId, characterId, count,
        mode, pageNumber)).thenReturn(ResponseEntity.ok(response));

    // when
    var characterResponse = sut.getCharacterActivityHistory(membershipType, membershipId,
        characterId, count, mode,
        pageNumber);

    // then
    Assertions.assertThat(characterResponse).isNotNull();
    Assertions.assertThat(characterResponse.getActivities()).isEmpty();

    // and
    verify(bungieClient, times(1))
        .getCharacterActivityHistory(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @MethodSource("emptyResponses")
  @DisplayName("Getting activity history for fails if the response is empty")
  public void activityHistoryNegativeTest(BungieResponse<ActivityHistoryResponse> response) {
    // given
    Integer membershipType = 3;
    Long membershipId = 1895002813L;
    Long characterId = 5782930231L;
    Integer count = 250;
    Integer mode = 4;
    Integer pageNumber = 0;

    when(bungieClient.getCharacterActivityHistory(membershipType, membershipId, characterId, count,
        mode, pageNumber)).thenReturn(ResponseEntity.ok(response));

    // when
    assertThrows(ExternalServiceException.class,
        () -> sut.getCharacterActivityHistory(membershipType, membershipId,
            characterId, count, mode,
            pageNumber),
        "The body of the paged-response is null or the Response element is null. "
        + "Parameters: membershipType [%s], membershipId [%s], characterId [%s], count [%s], mode [%s], page# [%s]"
            .formatted(membershipType, membershipId, characterId, count, mode, pageNumber));

    // and
    verify(bungieClient, times(1))
        .getCharacterActivityHistory(any(), any(), any(), any(), any(), any());
  }
}
