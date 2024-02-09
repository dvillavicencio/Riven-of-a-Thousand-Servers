package com.danielvm.destiny2bot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_indexing_information")
public class UserIndexing {

  @Id
  @Column(name = "user_discord_id")
  private Long discordUserId;

  @Column(name = "is_indexing")
  private Boolean isIndexing;

  @OneToMany(mappedBy = "userIndexing", cascade = CascadeType.ALL)
  private List<UserCharacterIndexing> currentCharacters;

}
