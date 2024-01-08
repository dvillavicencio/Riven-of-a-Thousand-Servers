package com.danielvm.destiny2bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.AuthorizedMessageFactory;
import com.danielvm.destiny2bot.factory.UserCharacterMessageCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorizedMessageRegistryTest {

  @Mock
  private UserCharacterMessageCreator userCharacterMessageCreator;

  @InjectMocks
  private AuthorizedMessageRegistry sut;

  @Test
  @DisplayName("Getting message creator for Raid stats command is successful")
  public void getMessageCreatorForRaidStats() {
    // given: raid stats command
    CommandEnum command = CommandEnum.RAID_STATS;

    // when: messageCreator is called
    AuthorizedMessageFactory creator = sut.messageCreator(command);

    // then: the creator is of the correct instance
    assertThat(creator).isInstanceOf(UserCharacterMessageCreator.class);
  }

  @Test
  @DisplayName("Getting message creator for non-existing command fails")
  public void getMessageCreatorFails() {
    // given: a command that does not require authorization
    CommandEnum command = CommandEnum.AUTHORIZE;

    // when: messageCreator is called an exception is thrown
    Assertions.assertThrows(ResourceNotFoundException.class,
        () -> sut.messageCreator(command),
        "No message creator found for command [%s]".formatted(command));
  }
}
