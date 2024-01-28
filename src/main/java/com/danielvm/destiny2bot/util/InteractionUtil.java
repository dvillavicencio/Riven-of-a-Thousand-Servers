package com.danielvm.destiny2bot.util;

import com.danielvm.destiny2bot.dto.discord.Option;
import com.danielvm.destiny2bot.exception.BadRequestException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;

public class InteractionUtil {

  private InteractionUtil() {
  }

  /**
   * Retrieves a specific option from a Discord interaction request from its list of options (if
   * applicable)
   *
   * @param options    The Discord options to search in
   * @param optionName The name of the option to look for
   * @return The dev-defined value of the option
   * @throws BadRequestException If the option specified by the optionName isn't found
   */
  public static String retrieveInteractionOption(List<Option> options, String optionName) {
    return String.valueOf(options.stream()
        .filter(o -> Objects.equals(optionName, o.getName())).findAny()
        .orElseThrow(() -> new BadRequestException(
            "No [%s] option present in the request".formatted(optionName),
            HttpStatus.BAD_REQUEST)).getValue());
  }
}
