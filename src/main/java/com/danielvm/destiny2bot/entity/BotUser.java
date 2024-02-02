package com.danielvm.destiny2bot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "bot_user")
public class BotUser {

  @Id
  @Column(name = "discord_id")
  private Long discordId;

  @Column(name = "discord_username")
  private String discordUsername;

  @Column(name = "bungie_membership_type")
  private Integer bungieMembershipType;

  @Column(name = "bungie_membership_id")
  private Long bungieMembershipId;

  @Column(name = "bungie_access_token")
  private String bungieAccessToken;

  @Column(name = "bungie_refresh_token")
  private String bungieRefreshToken;

  @Column(name = "bungie_token_expiration")
  private Long bungieTokenExpiration;

  @OneToMany(mappedBy = "botUser", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserCharacter> characters;
}
