package com.danielvm.destiny2bot.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.dto.destiny.membership.DestinyMembershipData;
import com.danielvm.destiny2bot.dto.destiny.membership.MembershipResponse;
import com.danielvm.destiny2bot.dto.destiny.membership.Memberships;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class BungieMembershipServiceTest {

  @Mock
  private BungieClient bungieClientMock;

  @InjectMocks
  private BungieMembershipService sut;

  @Test
  @DisplayName("Get membership for user should work as expected")
  public void testGetCurrentMembershipInfo() {
    // given
    var bearerToken = "SomeBearerToken";
    var membershipResponse = new MembershipResponse(
        new Memberships(List.of(new DestinyMembershipData(3, "membershipId"))));

    when(bungieClientMock.getMembershipForCurrentUser(bearerToken))
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

    when(bungieClientMock.getMembershipForCurrentUser(bearerToken))
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
    when(bungieClientMock.getMembershipForCurrentUser(bearerToken))
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
        new Memberships(List.of(new DestinyMembershipData(null, "membershipId"))));

    when(bungieClientMock.getMembershipForCurrentUser(bearerToken))
        .thenReturn(ResponseEntity.ok(membershipResponse));

    // when
    assertThrows(IllegalArgumentException.class,
        () -> sut.getCurrentUserMembershipInformation(bearerToken),
        "Membership Type is null for current user");
  }
}
