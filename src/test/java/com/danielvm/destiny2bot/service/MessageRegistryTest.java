package com.danielvm.destiny2bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.AuthorizeMessageCreator;
import com.danielvm.destiny2bot.factory.MessageResponseFactory;
import com.danielvm.destiny2bot.factory.WeeklyDungeonMessageCreator;
import com.danielvm.destiny2bot.factory.WeeklyRaidMessageCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessageRegistryTest {

  @Mock
  private WeeklyRaidMessageCreator weeklyRaidMessageCreator;
  @Mock
  private WeeklyDungeonMessageCreator weeklyDungeonMessageCreator;
  @Mock
  private AuthorizeMessageCreator authorizeMessageCreator;
  @InjectMocks
  private MessageRegistry sut;

  @Test
  @DisplayName("Getting message creator for authorize command works successfully")
  public void messageCreatorWorksForAuthorize() {
    // given: a valid command
    CommandEnum command = CommandEnum.AUTHORIZE;

    // when: messageCreator is called
    MessageResponseFactory creator = sut.messageCreator(command);

    // then: the correct message creator is returned
    assertThat(creator).isInstanceOf(AuthorizeMessageCreator.class);
  }

  @Test
  @DisplayName("Getting message creator for weekly dungeon command works successfully")
  public void messageCreatorWorksForWeeklyDungeon() {
    // given: a valid command
    CommandEnum command = CommandEnum.WEEKLY_DUNGEON;

    // when: messageCreator is called
    MessageResponseFactory creator = sut.messageCreator(command);

    // then: the correct message creator is returned
    assertThat(creator).isInstanceOf(WeeklyDungeonMessageCreator.class);
  }

  @Test
  @DisplayName("Getting message creator for weekly raid command works successfully")
  public void messageCreatorWorksForWeeklyRaid() {
    // given: a valid command
    CommandEnum command = CommandEnum.WEEKLY_RAID;

    // when: messageCreator is called
    MessageResponseFactory creator = sut.messageCreator(command);

    // then: the correct message creator is returned
    assertThat(creator).isInstanceOf(WeeklyRaidMessageCreator.class);
  }

  @Test
  @DisplayName("Getting message creator fails for authorized and invalid commands")
  public void messageCreatorFails() {
    // given: an authorized command
    CommandEnum command = CommandEnum.RAID_STATS;

    // when: messageCreator is called an exception is thrown
    Assertions.assertThrows(ResourceNotFoundException.class,
        () -> sut.messageCreator(command),
        "No message creator found for command [%s]".formatted(command));
  }

}
