package com.danielvm.destiny2bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_page_information")
public class UserPageInformation {

  @Id
  @Column(name = "user_discord_id")
  private Long userDiscordId;

  @Column(name = "number_of_pages")
  private Integer numberOfPages;

  @Column(name = "last_page")
  private Integer lastPage;
}
