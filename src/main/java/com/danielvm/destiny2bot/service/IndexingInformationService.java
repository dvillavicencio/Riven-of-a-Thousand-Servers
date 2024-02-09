package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.entity.UserCharacterIndexing;
import com.danielvm.destiny2bot.entity.UserIndexing;
import com.danielvm.destiny2bot.repository.UserIndexingRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IndexingInformationService {

  private final UserIndexingRepository userIndexingRepository;

  public IndexingInformationService(
      UserIndexingRepository userIndexingRepository) {
    this.userIndexingRepository = userIndexingRepository;
  }

  /**
   * Initiates a database entity for user indexing info with is_Indexing defaulted to true
   *
   * @param discordId The discordId of the entity to be created
   * @return the created {@link UserIndexing} entity
   */
  public UserIndexing initiateIndexing(Long discordId) {
    UserIndexing entity = new UserIndexing(discordId, true, new ArrayList<>());
    return userIndexingRepository.save(entity);
  }

  /**
   * Adds a user character indexing entity to a user-level indexing
   *
   * @param userIndexing          The user indexing to add a character-level indexing entity to
   * @param userCharacterIndexing The character-level entity to add
   */
  public void addCharacterInfo(UserIndexing userIndexing,
      UserCharacterIndexing userCharacterIndexing) {
    List<UserCharacterIndexing> currentCharacters = userIndexing.getCurrentCharacters();
    currentCharacters.add(userCharacterIndexing);
    userIndexing.setCurrentCharacters(currentCharacters);
    userIndexingRepository.save(userIndexing);
  }

  /**
   * Finalizes user indexing process for raid stats in the database and sets the indexing status to
   * false
   *
   * @param userIndexing The UserIndexing to modify/finalize
   */
  public void finalizeIndexing(UserIndexing userIndexing) {
    userIndexing.setIsIndexing(false);
    userIndexingRepository.save(userIndexing);
  }
}
