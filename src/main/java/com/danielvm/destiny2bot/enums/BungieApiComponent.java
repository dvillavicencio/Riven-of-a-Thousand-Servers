package com.danielvm.destiny2bot.enums;

import lombok.Getter;

public enum BungieApiComponent {

  PROFILES(100),
  VENDOR_RECEIPTS(101),
  PROFILE_INVENTORIES(102),
  CHARACTERS(200),
  CHARACTER_INVENTORIES(201),
  ITEM_INSTANCES(300),
  ITEM_PERKS(302),
  ITEM_STATS(304);

  @Getter
  private final Integer code;

  BungieApiComponent(Integer code) {
    this.code = code;
  }
}
