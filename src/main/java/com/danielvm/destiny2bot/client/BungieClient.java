package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.destiny.GenericResponse;
import com.danielvm.destiny2bot.dto.destiny.characters.CharactersResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ResponseFields;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.destiny.milestone.MilestoneEntry;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import reactor.core.publisher.Mono;

/**
 * This client is responsible for making calls to the Bungie
 */
public interface BungieClient {

  /**
   * Gets the membership info for the current user
   *
   * @param bearerToken The user's bearer token
   * @return {@link MembershipResponse}
   */
  @GetExchange("/User/GetMembershipsForCurrentUser/")
  ResponseEntity<MembershipResponse> getMembershipForCurrentUser(
      @RequestHeader(name = HttpHeaders.AUTHORIZATION) String bearerToken);

  /**
   * Gets the membership info for the current user in a reactive way
   *
   * @param bearerToken The user's bearer token
   * @return {@link MembershipResponse}
   */
  @GetExchange("/User/GetMembershipsForCurrentUser/")
  Mono<MembershipResponse> getMembershipInfoForCurrentUser(
      @RequestHeader(name = HttpHeaders.AUTHORIZATION) String bearerToken);

  /**
   * Ges a manifest entity from the Manifest API
   *
   * @param entityType     The entity type (see {@link ManifestEntity})
   * @param hashIdentifier The entity hash identifier
   * @return {@link GenericResponse} of {@link ResponseFields}
   */
  @GetExchange("/Destiny2/Manifest/{entityType}/{hashIdentifier}/")
  ResponseEntity<GenericResponse<ResponseFields>> getManifestEntity(
      @PathVariable(value = "entityType") String entityType,
      @PathVariable(value = "hashIdentifier") String hashIdentifier);

  /**
   * Ges a manifest entity from the Manifest API asynchronously
   *
   * @param entityType     The entity type (see {@link ManifestEntity})
   * @param hashIdentifier The entity hash identifier
   * @return {@link Mono} of {@link ResponseFields}
   */
  @GetExchange("/Destiny2/Manifest/{entityType}/{hashIdentifier}/")
  Mono<GenericResponse<ResponseFields>> getManifestEntityRx(
      @PathVariable(value = "entityType") String entityType,
      @PathVariable(value = "hashIdentifier") String hashIdentifier);

  /**
   * Get public Milestones
   *
   * @return {@link Mono} of Map of {@link MilestoneEntry}
   */
  @GetExchange("/Destiny2/Milestones/")
  Mono<GenericResponse<Map<String, MilestoneEntry>>> getPublicMilestonesRx();

  /**
   * Get a user characters
   *
   * @param membershipType      the membership type of the user
   * @param destinyMembershipId the destiny membership id of the user
   * @return {@link Mono} containing {@link CharactersResponse}
   */
  @GetExchange("/Destiny2/{membershipType}/Profile/{destinyMembershipId}/?components=200")
  Mono<GenericResponse<CharactersResponse>> getUserCharacters(
      @PathVariable Integer membershipType,
      @PathVariable String destinyMembershipId
  );

}
