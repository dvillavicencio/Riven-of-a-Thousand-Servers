package com.danielvm.destiny2bot.dao;

import com.danielvm.destiny2bot.entity.UserDetails;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserDetailsReactiveDao {

  private final ReactiveRedisTemplate<String, UserDetails> reactiveRedisTemplate;

  public UserDetailsReactiveDao(
      ReactiveRedisTemplate<String, UserDetails> reactiveRedisTemplate) {
    this.reactiveRedisTemplate = reactiveRedisTemplate;
  }

  /**
   * Save an entity to Redis if it was not previously present
   *
   * @param userDetails The entity to persist
   * @return True if it was successful, else returns False
   */
  public Mono<Boolean> save(UserDetails userDetails) {
    return reactiveRedisTemplate.opsForValue()
        .set(userDetails.getDiscordId(), userDetails);
  }

  /**
   * Return user details by their discordId
   *
   * @param discordId The discordId to find by
   * @return {@link UserDetails}
   */
  public Mono<UserDetails> getByDiscordId(String discordId) {
    return reactiveRedisTemplate.opsForValue().get(discordId);
  }

  /**
   * Returns if an entity with the given discordId exists
   *
   * @param discordId The discordId to check for
   * @return True if it exists, else False
   */
  public Mono<Boolean> existsByDiscordId(String discordId) {
    return reactiveRedisTemplate.opsForValue().get(discordId).hasElement();
  }
}
