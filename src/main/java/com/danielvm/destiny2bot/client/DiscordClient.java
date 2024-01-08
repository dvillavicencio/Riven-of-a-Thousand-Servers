package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import reactor.core.publisher.Mono;

public interface DiscordClient {

  /**
   * Gets the current Discord user details
   *
   * @param bearerToken The bearer token of the Discord user
   * @return {@link DiscordUserResponse}
   */
  @GetExchange("/users/@me")
  Mono<DiscordUserResponse> getUser(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken);
}
