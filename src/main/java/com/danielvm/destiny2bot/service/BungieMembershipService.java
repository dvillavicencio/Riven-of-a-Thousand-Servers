package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.util.MembershipUtil;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BungieMembershipService {

  private final BungieClient bungieClient;

  public BungieMembershipService(BungieClient bungieClient) {
    this.bungieClient = bungieClient;
  }

  /**
   * Get the current membership information for the currently logged-in user
   *
   * @param bearerToken The user's bearer token
   * @return {@link MembershipResponse}
   */
  public MembershipResponse getCurrentUserMembershipInformation(String bearerToken) {
    var membershipData = bungieClient.getMembershipForCurrentUser(
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
    return bungieClient.getMembershipInfoForCurrentUser(bearerToken)
        .filter(Objects::nonNull)
        .filter(membership ->
            Objects.nonNull(MembershipUtil.extractMembershipType(membership))
            && Objects.nonNull(MembershipUtil.extractMembershipId(membership)))
        .switchIfEmpty(Mono.error(
            new ResourceNotFoundException("Membership information for the user [%s] is invalid")));
  }

}
