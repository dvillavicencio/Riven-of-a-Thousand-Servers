package com.danielvm.destiny2bot.enums;

import lombok.Getter;

public enum ManifestEntity {

  BUCKET_DEFINITION("DestinyInventoryItemDefinition"),
  STAT_DEFINITION("DestinyStatDefinition"),
  CLASS_DEFINITION("DestinyClassDefinition"),
  ITEM_INVENTORY_DEFINITION("DestinyInventoryItemDefinition"),
  MILESTONE_DEFINITION("DestinyMilestoneDefinition"),
  ACTIVITY_TYPE_DEFINITION("DestinyActivityTypeDefinition"),
  ACTIVITY_DEFINITION("DestinyActivityDefinition");

  @Getter
  private final String id;

  ManifestEntity(String id) {
    this.id = id;
  }
}
