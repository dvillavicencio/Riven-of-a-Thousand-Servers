package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.discord.DiscordUserResponse;
import com.danielvm.destiny2bot.dto.discord.DmMessageRequest;
import com.danielvm.destiny2bot.dto.discord.DmMessageResponse;
import com.danielvm.destiny2bot.dto.discord.OpenDmChannelRequest;
import com.danielvm.destiny2bot.dto.discord.OpenDmChannelResponse;
import com.danielvm.destiny2bot.dto.oauth2.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * This client is responsible for making calls to Discord's API
 */
public interface DiscordClient {

  /**
   * Retrieves an access token from Discord's OAuth2's authorization server
   *
   * @param tokenExchangeParameters The required parameters for a token exchange
   * @return {@link TokenResponse}
   */
  @PostExchange(value = "/oauth2/token", accept = "application/json", contentType = "application/x-www-form-urlencoded")
  ResponseEntity<TokenResponse> getAccessToken(
      @RequestBody MultiValueMap<String, String> tokenExchangeParameters);

  /**
   * Gets the current Discord user details
   *
   * @param bearerToken The bearer token of the Discord user
   * @return {@link DiscordUserResponse}
   */
  @GetExchange("/users/@me")
  ResponseEntity<DiscordUserResponse> getUser(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken);

  /**
   * Open a DM channel with a discord user
   *
   * @param botToken             The Discord bot token
   * @param openDmChannelRequest The request containing the recipientId
   * @return {@link OpenDmChannelResponse}
   */
  @PostExchange("/users/@me/channels")
  ResponseEntity<OpenDmChannelResponse> openDmChannel(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String botToken,
      @RequestBody OpenDmChannelRequest openDmChannelRequest
  );

  /**
   * Send a discord Message through a DM channel
   *
   * @param channelId            The ID of the channel to send a DM through
   * @param botToken             The Discord bot token
   * @param openDmChannelRequest The request containing the recipientId
   * @return {@link OpenDmChannelResponse}
   */
  @PostExchange("/channels/{channelId}/messages")
  ResponseEntity<DmMessageResponse> sendDmMessage(
      @PathVariable Long channelId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String botToken,
      @RequestBody DmMessageRequest openDmChannelRequest
  );

  /**
   * Update a discord Message through a DM channel
   *
   * @param channelId            The ID of the channel to send a DM through
   * @param botToken             The Discord bot token
   * @param openDmChannelRequest The request containing the recipientId
   * @return {@link OpenDmChannelResponse}
   */
  @PatchExchange("/channels/{channelId}/messages/{messageId}")
  ResponseEntity<DmMessageResponse> updateDmMessage(
      @PathVariable Long channelId,
      @PathVariable Long messageId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String botToken,
      @RequestBody DmMessageRequest openDmChannelRequest
  );
}
