package com.danielvm.destiny2bot.enums;

import lombok.Getter;

public enum ActivityMode {

  STORY("story"),
  STRIKE("strike"),
  DUNGEON("dungeon"),
  RAID("raid");

  @Getter
  private final String label;

  ActivityMode(String label) {
    this.label = label;
  }
}
