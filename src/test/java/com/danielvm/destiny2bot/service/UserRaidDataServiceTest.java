package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.client.BungieManifestClient;
import com.danielvm.destiny2bot.repository.BotUserRepository;
import com.danielvm.destiny2bot.repository.CharacterRaidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserRaidDataServiceTest {

  @Mock
  private BotUserRepository botUserRepository;
  @Mock
  private BungieClient bungieClient;
  @Mock
  private BungieManifestClient bungieManifestClient;
  @Mock
  private BungieClient pgcrBungieClient;
  @Mock
  private CharacterRaidRepository characterRaidRepository;
  @Mock
  private IndexingStatusService indexingStatusService;

  @InjectMocks
  private UserRaidDataService sut;

  @Test
  @DisplayName("Load user details and characters is successful")
  public void loadUserDetailsAndCharactersIsSuccessful() {
    // given: membershipId, membershipType, and a BotUser entity
  }

}
