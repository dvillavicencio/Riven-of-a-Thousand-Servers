package com.danielvm.destiny2bot.util;

import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;

public class MembershipUtil {

  private MembershipUtil() {
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
