package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.destiny.BungieResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ResponseFields;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BungieClientWrapper {

  private final BungieClient defaultBungieClient;

  public BungieClientWrapper(BungieClient defaultBungieClient) {
    this.defaultBungieClient = defaultBungieClient;
  }

  /**
   * Wraps the client call to the Manifest with a Cacheable method
   *
   * @param entityType     The entity type (see {@link ManifestEntity})
   * @param hashIdentifier The hash identifier
   * @return {@link BungieResponse} of {@link ResponseFields}
   */
  @Cacheable(cacheNames = "entity", cacheManager = "inMemoryCacheManager")
  public Mono<BungieResponse<ResponseFields>> getManifestEntityRx(
      ManifestEntity entityType, String hashIdentifier) {
    return defaultBungieClient.getManifestEntityRx(entityType.getId(), hashIdentifier).cache();
  }

}
