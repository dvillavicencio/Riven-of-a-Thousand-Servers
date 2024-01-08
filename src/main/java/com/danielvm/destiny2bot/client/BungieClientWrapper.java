package com.danielvm.destiny2bot.client;

import com.danielvm.destiny2bot.dto.destiny.GenericResponse;
import com.danielvm.destiny2bot.dto.destiny.manifest.ResponseFields;
import com.danielvm.destiny2bot.enums.ManifestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BungieClientWrapper {

  private final BungieClient bungieClient;

  /**
   * Wraps the client call to the Manifest with a Cacheable method
   *
   * @param entityType     The entity type (see
   *                       {@link ManifestEntity})
   * @param hashIdentifier The hash identifier
   * @return {@link GenericResponse} of {@link ResponseFields}
   */
  @Cacheable(cacheNames = "entity", cacheManager = "inMemoryCacheManager")
  public Mono<GenericResponse<ResponseFields>> getManifestEntityRx(
      ManifestEntity entityType, String hashIdentifier) {
    return bungieClient.getManifestEntityRx(entityType.getId(), hashIdentifier).cache();
  }

}
