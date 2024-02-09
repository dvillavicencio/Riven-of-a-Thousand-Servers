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

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "user_character_indexing_information")
public class UserCharacterIndexing {

  @Id
  @Column(name = "character_id")
  private Long characterId;

  @Column(name = "last_page")
  private Integer lastPage;

  @ManyToOne
  @JoinColumn(name = "bot_user_id")
  private UserIndexing userIndexing;
}
