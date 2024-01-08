package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.BungieClient;
import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import org.mockito.InjectMocks;
import org.mockito.Mock;

// TODO: Write unit tests for character service
public class DestinyCharacterServiceTest {

  @Mock
  BungieClient bungieClient;

  @Mock
  UserDetailsReactiveDao userDetailsReactiveDao;

  @Mock
  BungieMembershipService bungieMembershipService;

  @InjectMocks
  DestinyCharacterService sut;



}
