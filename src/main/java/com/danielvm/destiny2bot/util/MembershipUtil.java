package com.danielvm.destiny2bot.util;

import com.danielvm.destiny2bot.dto.destiny.membership.DestinyMembershipData;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.exception.InternalServerException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
public class MembershipUtil {

  private static final Predicate<List<DestinyMembershipData>> NO_NULL_ELEMENTS = memberships ->
      memberships.stream()
          .anyMatch(data -> Objects.isNull(data.membershipId())
                            || Objects.isNull(data.membershipType()));

  private MembershipUtil() {
  }

  /**
   * Verify that the membership data response has all the needed fields
   *
   * @param membership      The membershipResponse to validate
   * @param discordUsername The discord username whose membership info needs validation
   * @return The verified response
   */
  public static ResponseEntity<MembershipResponse> verifyMembershipParameters(
      ResponseEntity<MembershipResponse> membership, String discordUsername) {
    if (Objects.isNull(membership) ||
        Objects.isNull(membership.getBody()) ||
        Objects.isNull(membership.getBody().response()) ||
        CollectionUtils.isEmpty(membership.getBody().response().destinyMemberships()) ||
        NO_NULL_ELEMENTS.test(membership.getBody().response().destinyMemberships())) {
      log.error("There were some missing parameters for the user's bungie membership response");
      throw new InternalServerException(
          "There were missing parameters for [%s]'s bungie membership".formatted(discordUsername),
          HttpStatus.BAD_GATEWAY);
    }
    return membership;
  }

  /**
   * Utility to extract the membershipId
   *
   * @param membership the membership response from Bungie
   * @return the membershipId
   */
  public static Long extractMembershipId(MembershipResponse membership) {
    return membership.response().destinyMemberships().get(0).membershipId();
  }

  /**
   * Utility to extract the membershipType
   *
   * @param membership the membership response from Bungie
   * @return the membershipType
   */
  public static Integer extractMembershipType(MembershipResponse membership) {
    return membership.response().destinyMemberships().get(0).membershipType();
  }
}
