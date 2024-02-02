package com.danielvm.destiny2bot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "character_raid")
public class CharacterRaid {

  @Id
  @Column(name = "instance_id")
  private Long instanceId;

  @Column(name = "raid_start_timestamp")
  private Instant raidStartTimestamp;

  @Column(name = "is_from_beginning")
  private Boolean isFromBeginning;

  @Column(name = "completed")
  private Boolean completed;

  @Column(name = "raid_name")
  private String raidName;

  @Column(name = "number_of_deaths")
  private Integer numberOfDeaths;

  @Column(name = "oponents_defeated")
  private Integer opponentsDefeated;

  @Column(name = "kill_death_assists")
  private Double kda;

  @Column(name = "raid_duration")
  private String raidDuration;

  @Column(name = "user_character_id")
  private Long userCharacterId;

  @OneToMany(mappedBy = "raidInstance", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RaidParticipant> participants;
}
