package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.model.state.ErrorState;
import com.danielvm.destiny2bot.model.state.IndexingState;
import com.danielvm.destiny2bot.model.state.SuccessState;
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
  @Mock
  private IndexingState indexingState;
  @Mock
  private SuccessState successState;
  @Mock
  private ErrorState errorState;

  @InjectMocks
  private DiscordStatusMessageService sut;



}
