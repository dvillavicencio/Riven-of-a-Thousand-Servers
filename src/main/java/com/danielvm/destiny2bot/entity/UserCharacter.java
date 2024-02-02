package com.danielvm.destiny2bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bungie_user_character")
@Builder
@Entity
public class UserCharacter {

  @Id
  @Column(name = "character_id")
  private Long characterId;

  @Column(name = "light_level")
  private Integer lightLevel;

  @Column(name = "destiny_class")
  private String destinyClass;

  @ManyToOne
  @JoinColumn(name = "discord_user_id", nullable = false)
  private BotUser botUser;
}
