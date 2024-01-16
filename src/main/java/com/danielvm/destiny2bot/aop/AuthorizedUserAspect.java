package com.danielvm.destiny2bot.aop;

import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.discord.Choice;
import com.danielvm.destiny2bot.dto.discord.Interaction;
import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.enums.InteractionResponseType;
import com.danielvm.destiny2bot.enums.InteractionType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Aspect
@Slf4j
@Component
public class AuthorizedUserAspect {

  private final UserDetailsReactiveDao userDetailsReactiveDao;

  public AuthorizedUserAspect(UserDetailsReactiveDao userDetailsReactiveDao) {
    this.userDetailsReactiveDao = userDetailsReactiveDao;
  }

  private static String validateInteractionAndExtractId(Interaction interaction) {
    Assert.notNull(interaction.getMember(), "Member data for the current target is null");

    return interaction.getMember().getUser().getId();
  }

//  /**
//   * Advice that matches any method within the factory package that's annotated with
//   * {@link com.danielvm.destiny2bot.annotation.Authorized} to check if they exist in Redis prior to
//   * making an authorized request to Bungie.net
//   *
//   * @param joinPoint   The matching jointPoint of this advice
//   * @param interaction The Discord interaction with this request
//   * @return {@link InteractionResponse}
//   */
//  @Before(value =
//      "execution(* com.danielvm.destiny2bot.factory..*(com.danielvm.destiny2bot.dto.discord.Interaction)) && "
//      + "@annotation(com.danielvm.destiny2bot.annotation.Authorized) && "
//      + "args(interaction)")
//  public Mono<InteractionResponse> verifyAuthorization(JoinPoint joinPoint,
//      Interaction interaction) {
//
//  }

  @Around(value =
      "execution(* com.danielvm.destiny2bot.factory..*(com.danielvm.destiny2bot.dto.discord.Interaction)) && "
      + "within(com.danielvm.destiny2bot.factory.*) && "
      + "@annotation(com.danielvm.destiny2bot.annotation.Authorized) && "
      + "args(interaction)")
  public Mono<Object> verifyAuthorization(
      ProceedingJoinPoint joinPoint, Interaction interaction) {
    InteractionType interactionType = InteractionType.findByValue(interaction.getType());
    return Mono.just(interaction)
        .map(AuthorizedUserAspect::validateInteractionAndExtractId)
        .flatMap(userDetailsReactiveDao::existsByDiscordId)
        .flatMap(exists -> {
          try {
            if (!exists) {
              return Mono.just(createErrorMessageResponse(interactionType));
            }
            return (Mono<?>) joinPoint.proceed();
          } catch (Throwable e) {
            return Mono.error(new RuntimeException(e));
          }
        });
  }

  private InteractionResponse createErrorMessageResponse(InteractionType interactionType) {
    return switch (interactionType) {
      case APPLICATION_COMMAND -> InteractionResponse.builder()
          .type(InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getType())
          .data(InteractionResponseData.builder()
              .content("Please refer to the /authorize command before using this command")
              .build())
          .build();
      case PING -> InteractionResponse.PING();
      case APPLICATION_COMMAND_AUTOCOMPLETE -> InteractionResponse.builder()
          .type(InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT.getType())
          .data(InteractionResponseData.builder()
              .choices(List.of(
                  new Choice("Please authorize", "Check /authorize for more!")))
              .build())
          .build();
      case MESSAGE_COMPONENT, MODAL_SUBMIT -> null; // Don't know what to do with these yet
    };
  }
}
