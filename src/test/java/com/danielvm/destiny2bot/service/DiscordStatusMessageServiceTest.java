package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DiscordStatusMessageServiceTest {

  @Mock
  private DiscordConfiguration discordConfiguration;

  @Mock
  private DiscordClient discordClient;

  @InjectMocks
  private DiscordStatusMessageService sut;


}
