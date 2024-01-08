package com.danielvm.destiny2bot.config;

import com.danielvm.destiny2bot.client.BungieClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Data
@Configuration
@ConfigurationProperties(prefix = "bungie.api")
public class BungieConfiguration implements OAuth2Configuration {

  /**
   * The name of the Bungie API key header
   */
  private static final String API_KEY_HEADER_NAME = "x-api-key";

  /**
   * API key provided by Bungie when registering an application in their portal
   */
  private String key;

  /**
   * Bungie clientId
   */
  private String clientId;

  /**
   * Bungie client secret
   */
  private String clientSecret;

  /**
   * Base url for Bungie Requests
   */
  private String baseUrl;

  /**
   * Url for Bungie Token endpoint
   */
  private String tokenUrl;

  /**
   * Url for OAuth2 authorization flow
   */
  private String authorizationUrl;

  /**
   * Url for callback during OAuth2 authorization
   */
  private String callbackUrl;

  @Bean
  public BungieClient bungieCharacterClient(WebClient.Builder builder) {
    var webClient = builder
        .baseUrl(this.baseUrl)
        .defaultHeader(API_KEY_HEADER_NAME, this.key)
        .build();
    return HttpServiceProxyFactory.builder()
        .exchangeAdapter(WebClientAdapter.create(webClient))
        .build()
        .createClient(BungieClient.class);
  }
}
