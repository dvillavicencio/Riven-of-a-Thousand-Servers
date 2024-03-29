package com.danielvm.destiny2bot.dto.destiny;

import com.danielvm.destiny2bot.dto.destiny.membership.DestinyMembershipData;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchResult {

  private String bungieGlobalDisplayName;

  private Integer bungieGlobalDisplayNameCode;

  private String bungieNetMembershipId;

  private List<DestinyMembershipData> destinyMemberships;
}
