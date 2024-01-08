package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.client.DiscordClient;
import com.danielvm.destiny2bot.config.BungieConfiguration;
import com.danielvm.destiny2bot.config.DiscordConfiguration;
import com.danielvm.destiny2bot.config.OAuth2Configuration;
import com.danielvm.destiny2bot.dao.UserDetailsReactiveDao;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import com.danielvm.destiny2bot.entity.UserDetails;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.util.OAuth2Util;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserRegistrationService {

  private static final String DISCORD_USER_ID_KEY = "discordUserId";
  private static final String DISCORD_USER_ALIAS_KEY = "discordUserAlias";

  private final DiscordConfiguration discordConfiguration;
  private final BungieConfiguration bungieConfiguration;
  private final DiscordClient discordClient;
  private final UserDetailsReactiveDao userDetailsReactiveDao;
  private final WebClient.Builder defaultWebClientBuilder;


  public UserRegistrationService(
      DiscordConfiguration discordConfiguration,
      BungieConfiguration bungieConfiguration,
      DiscordClient discordClient,
      UserDetailsReactiveDao userDetailsReactiveDao,
      WebClient.Builder defaultWebClientBuilder) {
    this.discordConfiguration = discordConfiguration;
    this.bungieConfiguration = bungieConfiguration;
    this.discordClient = discordClient;
    this.userDetailsReactiveDao = userDetailsReactiveDao;
    this.defaultWebClientBuilder = defaultWebClientBuilder;
  }

  /**
   * Retrieve DiscordUserId from authenticated user and save it to Session
   *
   * @param authorizationCode The authorization code from Discord
   * @param session           The HttpSession the user is linked to
   * @return ResponseEntity with redirection to begin Bungie OAuth2 flow
   */
  public Mono<ResponseEntity<Object>> authenticateDiscordUser(String authorizationCode,
      HttpSession session) {
    return getTokenResponse(authorizationCode, discordConfiguration)
        .flatMap(token -> discordClient.getUser(
            OAuth2Util.formatBearerToken(token.getAccessToken())))
        .switchIfEmpty(
            Mono.error(new IllegalStateException("The user response from Discord is empty")))
        .flatMap(token -> {
          if (Objects.isNull(token.getId()) || Objects.isNull(token.getUsername())) {
            var errorMessage = "Some required arguments for registration are null for the current user";
            return Mono.error(new IllegalStateException(errorMessage));
          }
          return Mono.just(token);
        })
        .doOnSuccess(user -> {
          session.setAttribute(DISCORD_USER_ALIAS_KEY, user.getUsername());
          session.setAttribute(DISCORD_USER_ID_KEY, user.getId());
        })
        .then(Mono.just(ResponseEntity
            .status(HttpStatus.FOUND) // on success relocate to Bungie Auth URL
            .header(HttpHeaders.LOCATION, OAuth2Util.bungieAuthorizationUrl(
                bungieConfiguration.getAuthorizationUrl(),
                bungieConfiguration.getClientId()))
            .build())
        );
  }

  /**
   * Link the Bungie credentials to the current DiscordUserId and create a database entry
   *
   * @param authorizationCode The authorization code from Bungie
   * @param httpSession       The HttpSession the user is linked to
   * @return ResponseEntity with no content when the user is persisted successfully
   */
  public Mono<ResponseEntity<Object>> linkDiscordUserToBungieAccount(String authorizationCode,
      HttpSession httpSession) {
    return getTokenResponse(authorizationCode, bungieConfiguration)
        .flatMap(token -> {
          UserDetails userDetails = UserDetails.builder()
              .discordUsername((String) httpSession.getAttribute(DISCORD_USER_ALIAS_KEY))
              .discordId((String) httpSession.getAttribute(DISCORD_USER_ID_KEY))
              .accessToken(token.getAccessToken())
              .refreshToken(token.getRefreshToken())
              .expiration(Instant.now().plusSeconds(token.getExpiresIn()))
              .build();
          httpSession.invalidate();
          return userDetailsReactiveDao.save(userDetails);
        })
        .flatMap(result -> result ? Mono.empty() : Mono.error(
            new InternalServerException("Something went wrong when persisting a user into Redis",
                HttpStatus.INTERNAL_SERVER_ERROR)))
        .then(Mono.just(ResponseEntity.noContent().build()));
  }

  private Mono<TokenResponse> getTokenResponse(String authorizationCode,
      OAuth2Configuration oAuth2Configuration) {
    MultiValueMap<String, String> map =
        OAuth2Util.buildTokenExchangeParameters(authorizationCode,
            oAuth2Configuration.getCallbackUrl(), oAuth2Configuration.getClientSecret(),
            oAuth2Configuration.getClientId());

    var tokenClient = defaultWebClientBuilder
        .baseUrl(oAuth2Configuration.getTokenUrl())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();

    return tokenClient.post()
        .body(BodyInserters.fromFormData(map))
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .flatMap(token -> {
          if (Objects.isNull(token.getAccessToken()) || Objects.isNull(token.getRefreshToken())
              || Objects.isNull(token.getExpiresIn())) {
            String errorMessage = "The access token, the refresh token or the expiration are null for user";
            return Mono.error(new IllegalStateException(errorMessage));
          }
          return Mono.just(token);
        });
  }

}
