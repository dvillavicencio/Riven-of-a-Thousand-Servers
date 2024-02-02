package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ResponseFields;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * The difference between this bungie client and the other bungie clients in the project is that
 * this one caches all the already used requests and responses we get from Bungie. This happens a
 * lot when calling the manifest database with the same parameters over and over again.
 */
@Service
@Slf4j
public class BungieManifestClient {

  private final BungieClient reactiveClient;
  private final BungieClient imperativeClient;

  public BungieManifestClient(
      BungieClient reactiveBungieClient,
      BungieClient imperativeBungieClient) {
    this.reactiveClient = reactiveBungieClient;
    this.imperativeClient = imperativeBungieClient;
  }

  /**
   * Wraps the client call to the Manifest with a Cacheable method
   *
   * @param entityType     The entity type (see {@link ManifestEntity})
   * @param hashIdentifier The hash identifier
   * @return {@link BungieResponse} of {@link ResponseFields}
   */
  @Cacheable(cacheNames = "entityReactive", cacheManager = "inMemoryCacheManager")
  public Mono<BungieResponse<ResponseFields>> getManifestEntityRx(
      ManifestEntity entityType, String hashIdentifier) {
    return reactiveClient.getManifestEntityRx(entityType.getId(), hashIdentifier).cache();
  }

  /**
   * Wraps the client call to the Manifest with a Cacheable method with the imperative Rest Client
   *
   * @param entityType     The entity type (see {@link ManifestEntity})
   * @param hashIdentifier The hash identifier
   * @return {@link BungieResponse} of {@link ResponseFields}
   */
  @Cacheable(cacheNames = "entityImperative", cacheManager = "inMemoryCacheManager")
  public ResponseEntity<BungieResponse<ResponseFields>> getManifestEntity(
      ManifestEntity entityType, Long hashIdentifier) {
    return imperativeClient.getManifestEntity(entityType.getId(), hashIdentifier);
  }

}
