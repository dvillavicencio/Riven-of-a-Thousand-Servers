package com.danielvm.destiny2bot.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
public class OAuth2Util {

  private static final String BEARER_TOKEN_FORMAT = "Bearer %s";

  private OAuth2Util() {
  }

  /**
   * Format the given token to bearer token format
   *
   * @param token The token to format
   * @return The formatted String
   */
  public static String formatBearerToken(String token) {
    return BEARER_TOKEN_FORMAT.formatted(token);
  }

  /**
   * Build body parameters for a token request to an OAuth2 resource provider
   *
   * @param authorizationCode The authorization code
   * @param redirectUri       The redirect type
   * @param clientSecret      The client secret
   * @param clientId          The clientId
   * @return {@link MultiValueMap} of OAuth2 attributes for Token exchange
   */
  public static MultiValueMap<String, String> buildTokenExchangeParameters(
      String authorizationCode,
      String redirectUri,
      String clientSecret,
      String clientId) {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add(OAuth2Params.CODE, authorizationCode);
    map.add(OAuth2Params.GRANT_TYPE, OAuth2Params.AUTHORIZATION_CODE);
    map.add(OAuth2Params.REDIRECT_URI, redirectUri);
    map.add(OAuth2Params.CLIENT_SECRET, clientSecret);
    map.add(OAuth2Params.CLIENT_ID, clientId);
    return map;
  }

  /**
   * Build body parameters for a token request to an OAuth2 resource provider
   *
   * @param refreshToken The refresh token to send
   * @return {@link MultiValueMap} of OAuth2 attributes for refresh token exchange
   */
  public static MultiValueMap<String, String> buildRefreshTokenExchangeParameters(
      String refreshToken, String clientId, String clientSecret) {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add(OAuth2Params.GRANT_TYPE, OAuth2Params.REFRESH_TOKEN);
    map.add(OAuth2Params.REFRESH_TOKEN, refreshToken);
    map.add(OAuth2Params.CLIENT_ID, clientId);
    map.add(OAuth2Params.CLIENT_SECRET, clientSecret);
    return map;
  }

  /**
   * Return the qualified url with all parameters
   *
   * @param authUrl  The auth URL parameter
   * @param clientId The clientId for the OAuth2 application
   * @return The authorization url with all required parameters
   */
  public static String bungieAuthorizationUrl(String authUrl, String clientId) {
    return UriComponentsBuilder
        .fromHttpUrl(authUrl)
        .queryParam(OAuth2Params.RESPONSE_TYPE, OAuth2Params.CODE)
        .queryParam(OAuth2Params.CLIENT_ID, clientId)
        .build().toString();
  }

  /**
   * Returns a registration link based on some OAuth2 parameters. This URL can be used to register
   * the bot to an end-user's Discord server
   *
   * @param authUrl     The authorization URL parameter
   * @param clientId    The clientId parameter
   * @param callbackUrl The callback URL parameter
   * @param scopes      The scopes parameter
   * @return a String with the above parameters
   */
  public static String discordAuthorizationUrl(
      String authUrl, String clientId, String callbackUrl, String scopes) {
    return UriComponentsBuilder.fromHttpUrl(authUrl)
        .queryParam(OAuth2Params.CLIENT_ID, clientId)
        .queryParam(OAuth2Params.REDIRECT_URI,
            URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8))
        .queryParam(OAuth2Params.RESPONSE_TYPE, OAuth2Params.CODE)
        .queryParam(OAuth2Params.SCOPE, scopes).build().toString();
  }

}
