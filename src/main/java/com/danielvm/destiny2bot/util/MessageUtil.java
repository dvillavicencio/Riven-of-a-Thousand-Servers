package com.danielvm.destiny2bot.util;

import static com.danielvm.destiny2bot.enums.InteractionResponse.CHANNEL_MESSAGE_WITH_SOURCE;

import com.danielvm.destiny2bot.dto.discord.InteractionResponse;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;


public class MessageUtil {

  private static final LocalTime DESTINY_2_STANDARD_RESET_TIME = LocalTime.of(9, 0);
  private static final ZoneId STANDARD_TIMEZONE = ZoneId.of("America/Los_Angeles");
  public static final ZonedDateTime NEXT_TUESDAY = ZonedDateTime.of(
          LocalDate.now(STANDARD_TIMEZONE), DESTINY_2_STANDARD_RESET_TIME, STANDARD_TIMEZONE)
      .with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
  public static final ZonedDateTime PREVIOUS_TUESDAY = ZonedDateTime.of(
          LocalDate.now(STANDARD_TIMEZONE), DESTINY_2_STANDARD_RESET_TIME, STANDARD_TIMEZONE)
      .with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));

  /**
   * Formatter used to format the date for a message Example: the LocalDate object with date
   * 2023-01-01 would be formatted to "Sunday 1st, January 2023"
   */
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
      "EEEE d'%s', MMMM yyyy");

  private MessageUtil() {
  }

  /**
   * Format the given local date using the class formatter, it also includes the correct suffix for
   * a date
   *
   * @param localDate the date to be formatted
   * @return String of the formatted local date
   */
  public static String formatDate(LocalDate localDate) {
    Integer dayOfMonth = localDate.getDayOfMonth();
    return FORMATTER.format(localDate)
        .formatted(suffix(dayOfMonth));
  }

  private static String suffix(Integer dayOfMonth) {
    if (dayOfMonth >= 11 && dayOfMonth <= 13) {
      return "th";
    }
    return switch (dayOfMonth % 10) {
      case 1 -> "st";
      case 2 -> "nd";
      case 3 -> "rd";
      default -> "th";
    };
  }

  /**
   * Construct a default error message for use of commands that require authorization
   *
   * @return {@link com.danielvm.destiny2bot.dto.discord.InteractionResponse}
   */
  public static InteractionResponse unauthorizedUserMessage() {
    return InteractionResponse.builder()
        .type(CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(InteractionResponseData.builder()
            .content("""
                In order to use this wish you first need to authorize Riven to do so.
                Please refer to `/authorize` command.
                """)
            .build())
        .build();
  }
}
