package com.danielvm.destiny2bot.dto.destiny.manifest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManifestFields {

  private DisplayProperties displayProperties;

  private Stats stats;

  private EquipmentBlock equipmentBlock;

  private String defaultDamageType;

  private Integer itemType;

  private Integer itemSubType;

  private Integer directActivityModeType;

  private String activityTypeHash;

  private Long hash;
}
