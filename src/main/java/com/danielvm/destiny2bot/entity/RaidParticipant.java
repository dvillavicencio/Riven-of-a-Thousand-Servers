package com.danielvm.destiny2bot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "raid_participant")
public class RaidParticipant {

  @Id
  @Column(name = "membership_id")
  private Long membershipId;

  @Column(name = "username")
  private String username;

  @Column(name = "character_class")
  private String characterClass;

  @Column(name = "icon_path")
  private String iconPath;

  @Column(name = "completed")
  private Boolean completed;

  @JoinColumn(name = "raid_instance")
  @ManyToOne(cascade = CascadeType.REMOVE)
  private CharacterRaid raidInstance;
}
