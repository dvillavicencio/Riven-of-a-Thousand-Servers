package com.danielvm.destiny2bot.dto.destiny;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDetails {

  private Long referenceId;

  private Long instanceId;

  private Boolean isPrivate;

  private Integer membershipType;

  private Long directorActivityHash;
}
