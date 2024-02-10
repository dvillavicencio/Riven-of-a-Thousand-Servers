package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.dto.destiny.ActivityHistoryResponse;
import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.PostGameCarnageReport;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ManifestFields;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import com.danielvm.destiny2bot.exception.ExternalServiceException;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.util.MembershipUtil;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BungieAPIService {

  private final BungieClient reactiveBungieClient;
  private final BungieClient imperativeBungieClient;
  private final BungieClient pgcrBungieClient;

  public BungieAPIService(
      BungieClient reactiveBungieClient,
      BungieClient imperativeBungieClient,
      BungieClient pgcrBungieClient) {
    this.reactiveBungieClient = reactiveBungieClient;
    this.imperativeBungieClient = imperativeBungieClient;
    this.pgcrBungieClient = pgcrBungieClient;
  }

  /**
   * Get the current membership information for the currently logged-in user
   *
   * @param bearerToken The user's bearer token
   * @return {@link MembershipResponse}
   */
  public MembershipResponse getCurrentUserMembershipInformation(String bearerToken) {
    var membershipData = imperativeBungieClient.getMembershipForCurrentUser(
        bearerToken).getBody();

    Assert.notNull(membershipData, "The membership characters for the current user is null");
    Assert.notNull(MembershipUtil.extractMembershipId(membershipData),
        "Membership Id is null for current user");
    Assert.notNull(MembershipUtil.extractMembershipType(membershipData),
        "Membership Type is null for current user");
    return membershipData;
  }

  /**
   * Retrieves membership information for a bungie user
   *
   * @param bearerToken the user's bearer token
   * @return {@link MembershipResponse}
   */
  public Mono<MembershipResponse> getUserMembershipInformation(String bearerToken) {
    return reactiveBungieClient.getMembershipInfoForCurrentUser(bearerToken)
        .filter(Objects::nonNull)
        .filter(membership ->
            MembershipUtil.extractMembershipType(membership) != null
            && MembershipUtil.extractMembershipId(membership) != null)
        .switchIfEmpty(Mono.error(
            new ResourceNotFoundException("Membership information for the user [%s] is invalid")));
  }

  /**
   * Retrieves a manifest entity from the Bungie API
   *
   * @param entityType     The type of the manifest entity to retrieve (see {@link ManifestEntity})
   * @param hashIdentifier The hash of the manifest entity to retrieve
   * @return {@link ManifestFields}
   */
  @Cacheable(cacheNames = "entityImperative", cacheManager = "inMemoryCacheManager")
  public ManifestFields getManifestEntity(
      ManifestEntity entityType, Long hashIdentifier) {
    var response = imperativeBungieClient.getManifestEntity(entityType.getId(), hashIdentifier)
        .getBody();
    if (Objects.isNull(response) || Objects.isNull(response.getResponse())) {
      String errorMessage = "The manifest entity for activity of type [%s] with hash [%s] returned a null for a required field"
          .formatted(entityType, hashIdentifier);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }
    return response.getResponse();
  }

  /**
   * Retrieves a Post Game Carnage Report for an activity instance from Bungie's API
   *
   * @param accessToken      The user's access token
   * @param activityInstance The instance of the activity
   * @return {@link PostGameCarnageReport}
   */
  public PostGameCarnageReport getPGCR(String accessToken, Long activityInstance) {
    BungieResponse<PostGameCarnageReport> report = pgcrBungieClient.getPostCarnageReport(
        OAuth2Util.formatBearerToken(accessToken), activityInstance
    ).getBody();
    if (report == null || report.getResponse() == null) {
      String errorMessage = "A PGCR for a given raid did not include some required details. Raid instance [%s]"
          .formatted(activityInstance);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }
    return report.getResponse();
  }

  /**
   * Retrieves Destiny 2 characters for a bungie user
   *
   * @param membershipType The membershipType of a user
   * @param membershipId   The membershipId of a user
   * @return {@link CharactersResponse}
   */
  public CharactersResponse getCharacters(Integer membershipType, Long membershipId) {
    BungieResponse<CharactersResponse> characters = imperativeBungieClient.getUserCharacters(
        membershipType, membershipId).getBody();
    if (characters == null || characters.getResponse() == null ||
        characters.getResponse().getCharacters() == null ||
        characters.getResponse().getCharacters().getData() == null) {
      String errorMessage = "The character response for user with membership type [%s] and membershipId [%s] is missing required fields"
          .formatted(membershipType, membershipId);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }
    return characters.getResponse();
  }

  /**
   * Retrieves a paged response for a character's activity history
   *
   * @param membershipType The user's membershipType
   * @param membershipId   The user's membershipId
   * @param characterId    The ID of the character to retrieve history for
   * @param count          The count of how many elements should appear in the page
   * @param mode           The activity mode type
   * @param pageNumber     The page you want to retrieve
   * @return {@link ActivityHistoryResponse}
   */
  public ActivityHistoryResponse getCharacterActivityHistory(Integer membershipType,
      Long membershipId, Long characterId, Integer count, Integer mode, Integer pageNumber) {
    BungieResponse<ActivityHistoryResponse> response = imperativeBungieClient.getCharacterActivityHistory(
        membershipType, membershipId, characterId, count, mode, pageNumber).getBody();
    if (response == null || response.getResponse() == null) {
      String errorMessage = (
          "The body of the paged-response is null or the Response element is null. "
          + "Parameters: membershipType [%s], membershipId [%s], characterId [%s], count [%s], mode [%s], page# [%s]")
          .formatted(membershipType, membershipId, characterId, characterId, mode, pageNumber);
      log.error(errorMessage);
      throw new ExternalServiceException(errorMessage);
    }
    return response.getResponse();
  }

}
