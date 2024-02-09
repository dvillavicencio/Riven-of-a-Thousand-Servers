package com.danielvm.destiny2bot.factory.creator;

import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.service.UserCharacterService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RaidStatsMessageCreator implements ApplicationCommandSource,
    AutocompleteSource {

  private static final String CHOICE_FORMAT = "[%s] %s - %s";
  private final UserCharacterService userCharacterService;

  public RaidStatsMessageCreator(
      UserCharacterService userCharacterService) {
    this.userCharacterService = userCharacterService;
  }

  @Override
  public Mono<InteractionResponse> createResponse(Interaction interaction) {
    return null;
  }

  @Override
  public Mono<InteractionResponse> autocompleteResponse(Interaction interaction) {
    String userId = interaction.getMember().getUser().getId();
    return userCharacterService.getCharactersForUser(userId)
        .map(character -> new Choice(CHOICE_FORMAT.formatted(
            character.getLightLevel(), character.getCharacterRace(), character.getCharacterClass()),
            character.getCharacterId()))
        .collectList()
        .map(choices -> {
          if (choices.size() > 1) {
            choices.add(new Choice("All", "Gets stats for all characters"));
          }
          return new InteractionResponse(8, InteractionResponseData.builder()
              .choices(choices)
              .build());
        });
  }
}
