package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.enums.InteractionType;
import com.danielvm.destiny2bot.enums.SlashCommand;
import com.danielvm.destiny2bot.factory.ApplicationCommandFactory;
import com.danielvm.destiny2bot.factory.AutocompleteFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InteractionService {

  private final ApplicationCommandFactory applicationCommandFactory;
  private final AutocompleteFactory autocompleteFactory;

  public InteractionService(
      ApplicationCommandFactory applicationCommandFactory,
      AutocompleteFactory autocompleteFactory) {
    this.applicationCommandFactory = applicationCommandFactory;
    this.autocompleteFactory = autocompleteFactory;
  }

  /**
   * Handles the incoming interactions from Discord using each slash-command's appropriate message
   * factory
   *
   * @param interaction The received interaction from the Discord chat
   * @return {@link InteractionResponse}
   */
  public Mono<InteractionResponse> handleInteraction(Interaction interaction) {
    log.info("Interaction received: {}", interaction);
    InteractionType interactionType  = InteractionType.findByValue(interaction.getType());
    return switch (interactionType) {
      case MODAL_SUBMIT, MESSAGE_COMPONENT -> Mono.just(new InteractionResponse());
      case APPLICATION_COMMAND_AUTOCOMPLETE -> {
        SlashCommand command = SlashCommand.findByName(interaction.getData().getName());
        yield autocompleteFactory.messageCreator(command).autocompleteResponse(interaction);
      }
      case APPLICATION_COMMAND -> {
        SlashCommand command = SlashCommand.findByName(interaction.getData().getName());
        yield applicationCommandFactory.messageCreator(command).createResponse(interaction);
      }
      case PING -> Mono.just(InteractionResponse.PING());
    };
  }

}
