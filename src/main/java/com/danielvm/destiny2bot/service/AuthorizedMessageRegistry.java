package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.exception.ResourceNotFoundException;
import com.danielvm.destiny2bot.factory.AuthorizedMessageFactory;
import com.danielvm.destiny2bot.factory.MessageResponseFactory;
import com.danielvm.destiny2bot.factory.UserCharacterMessageCreator;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AuthorizedMessageRegistry {

  private final Map<CommandEnum, AuthorizedMessageFactory> autocompleteMessageFactory;

  public AuthorizedMessageRegistry(
      UserCharacterMessageCreator userCharacterMessageCreator) {
    this.autocompleteMessageFactory = Map.of(
        CommandEnum.RAID_STATS, userCharacterMessageCreator
    );
  }

  /**
   * Return the corresponding message-creator associated with a Command in {@link CommandEnum}
   *
   * @param command The command to get the factory for
   * @return an implementation of {@link MessageResponseFactory}
   * @throws ResourceNotFoundException If no creator is found for the given command
   */
  public AuthorizedMessageFactory messageCreator(CommandEnum command) {
    AuthorizedMessageFactory creator = autocompleteMessageFactory.get(command);
    if (Objects.isNull(creator)) {
      throw new ResourceNotFoundException(
          "No message creator found for command [%s]".formatted(command));
    }
    return creator;
  }

}
