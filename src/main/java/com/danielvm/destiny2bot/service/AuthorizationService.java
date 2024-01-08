package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.util.OAuth2Util;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthorizationService {

  private final UserDetailsReactiveDao userDetailsReactiveDao;
  private final BungieConfiguration bungieConfiguration;
  private final WebClient.Builder webClient;

  public AuthorizationService(
      UserDetailsReactiveDao userDetailsReactiveDao,
      BungieConfiguration bungieConfiguration,
      Builder webClient) {
    this.userDetailsReactiveDao = userDetailsReactiveDao;
    this.bungieConfiguration = bungieConfiguration;
    this.webClient = webClient;
  }

  /**
   * Returns whether there exists a user with the given id in Redis
   *
   * @param userId the userId to search for
   * @return True if it exists, else False
   */
  public Mono<Boolean> isUserAuthorized(String userId) {
    return userDetailsReactiveDao.existsByDiscordId(userId);
  }

  /**
   * Returns whether the access token of the user is expired
   *
   * @param userId the userId for which to check the access token
   * @return True if is expired, else False
   */
  public Mono<Boolean> accessTokenNeedsRefresh(String userId) {
    return userDetailsReactiveDao.getByDiscordId(userId)
        .map(userDetails -> {
          var expiration = userDetails.getExpiration();
          return expiration.isAfter(Instant.now());
        });
  }

  /**
   * Check if the access token saved for a given user needs refreshing. If it does, then refresh it.
   * Else don't do anything.
   *
   * @param userId the userId of the owner of the access token
   */
  public void refreshToken(String userId) {
    accessTokenNeedsRefresh(userId)
        .doOnSuccess(refresh -> log.info("Access token for user [%s] requires refreshing: [%s]"
            .formatted(userId, refresh)))
        .zipWith(userDetailsReactiveDao.getByDiscordId(userId), (needsRefresh, userDetails) -> {
          if (needsRefresh) {
            MultiValueMap<String, String> parameters =
                OAuth2Util.buildRefreshTokenExchangeParameters(
                    userDetails.getRefreshToken(),
                    bungieConfiguration.getClientId(),
                    bungieConfiguration.getClientSecret()
                );

            WebClient refreshClient = webClient.baseUrl(bungieConfiguration.getTokenUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

            return refreshClient.post()
                .body(BodyInserters.fromFormData(parameters))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(token -> userDetailsReactiveDao.getByDiscordId(userId)
                    .map(entity -> {
                      entity.setAccessToken(token.getAccessToken());
                      entity.setRefreshToken(token.getRefreshToken());
                      entity.setExpiration(Instant.now().plusSeconds(token.getExpiresIn()));
                      return userDetailsReactiveDao.save(entity);
                    }));
          }
          return Mono.empty();
        })
        .subscribe();
  }
}
