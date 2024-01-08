package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.enums.CommandEnum;
import com.danielvm.destiny2bot.enums.InteractionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InteractionService {

  private final MessageRegistry messageRegistry;
  private final AuthorizedMessageRegistry authorizedMessageRegistry;

  public InteractionService(
      AuthorizedMessageRegistry authorizedMessageRegistry,
      MessageRegistry messageRegistry) {
    this.messageRegistry = messageRegistry;
    this.authorizedMessageRegistry = authorizedMessageRegistry;
  }

  /**
   * Handles the incoming interactions from Discord using each slash-command's appropriate
   * message-creator using {@link MessageRegistry} or {@link AuthorizedMessageRegistry} depending on
   * if the given slash command needs the user to be authorized
   *
   * @param interaction The received interaction from the Discord chat
   * @return {@link InteractionResponse}
   */
  public Mono<InteractionResponse> handleInteraction(Interaction interaction) {
    InteractionType interactionType = InteractionType.findByValue(interaction.getType());
    return switch (interactionType) {
      case MESSAGE_COMPONENT, MODAL_SUBMIT -> Mono.just(new InteractionResponse());
      case APPLICATION_COMMAND, APPLICATION_COMMAND_AUTOCOMPLETE -> {
        CommandEnum command = CommandEnum.findByName(interaction.getData().getName());
        if (command.isAuthorized()) {
          Assert.notNull(interaction.getMember().getUser().getId(),
              "Member information is null for given command [%s] and command type [%s]"
                  .formatted(command.getCommandName(), interactionType));

          String userId = interaction.getMember().getUser().getId();
          yield authorizedMessageRegistry.messageCreator(command).createResponse(userId);
        } else {
          yield messageRegistry.messageCreator(command).createResponse();
        }
      }
      case PING -> InteractionResponse.PING();
    };
  }
}
