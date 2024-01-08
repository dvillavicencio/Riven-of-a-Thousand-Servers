package com.danielvm.destiny2bot.dao;

import static org.mockito.Mockito.when;

import com.danielvm.destiny2bot.entity.UserDetails;
import java.util.Objects;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class UserDetailsReactiveDaoTest {

  @Mock
  public ReactiveRedisTemplate<String, UserDetails> reactiveRedisTemplate;
  @InjectMocks
  public UserDetailsReactiveDao sut;
  @Mock
  ReactiveValueOperations<String, UserDetails> reactiveValueOperations;

  @Test
  @DisplayName("Saving an entity to redis is successful")
  public void saveIsSuccessful() {
    // given: a UserDetails entity to save
    String discordId = "someDiscordId";
    UserDetails entity = UserDetails.builder()
        .discordId(discordId)
        .accessToken("accessToken")
        .refreshToken("refreshToken")
        .discordUsername("discordUsername")
        .build();

    when(reactiveRedisTemplate.opsForValue())
        .thenReturn(reactiveValueOperations);

    when(reactiveValueOperations.set(discordId, entity))
        .thenReturn(Mono.just(Boolean.TRUE));

    // when: save is called
    var response = StepVerifier.create(sut.save(entity));

    // then: save returns TRUE
    response.assertNext(result -> Assertions.assertThat(result).isTrue())
        .verifyComplete();
  }

  @Test
  @DisplayName("Saving an entity to Redis fails")
  public void saveFails() {
    // given: a UserDetails entity to save
    String discordId = "someDiscordId";
    UserDetails entity = UserDetails.builder()
        .discordId(discordId)
        .accessToken("accessToken")
        .refreshToken("refreshToken")
        .discordUsername("discordUsername")
        .build();

    when(reactiveRedisTemplate.opsForValue())
        .thenReturn(reactiveValueOperations);

    when(reactiveValueOperations.set(discordId, entity))
        .thenReturn(Mono.just(Boolean.FALSE));

    // when: save is called
    var response = StepVerifier.create(sut.save(entity));

    // then: save returns FALSE when it fails to save to Redis
    response
        .assertNext(result -> Assertions.assertThat(result).isFalse())
        .verifyComplete();
  }

  @Test
  @DisplayName("Getting an entity by their DiscordId is successful")
  public void retrieveByDiscordId() {
    // given: a discordId to retrieve by
    String discordId = "someDiscordId";
    UserDetails entity = UserDetails.builder()
        .discordId(discordId)
        .accessToken("accessToken")
        .refreshToken("refreshToken")
        .discordUsername("discordUsername")
        .build();

    when(reactiveRedisTemplate.opsForValue())
        .thenReturn(reactiveValueOperations);

    when(reactiveValueOperations.get(discordId))
        .thenReturn(Mono.just(entity));

    // when: getByDiscordId is called
    var response = StepVerifier.create(sut.getByDiscordId(discordId));

    // then: returns the correct entity
    Predicate<UserDetails> assertions = userDetails ->
        Objects.equals(userDetails.getDiscordUsername(), "discordUsername") &&
        Objects.equals(userDetails.getRefreshToken(), "refreshToken") &&
        Objects.equals(userDetails.getDiscordId(), discordId);

    Condition<UserDetails> condition = new Condition<>(assertions, "User details matches");
    response
        .assertNext(result ->
            Assertions.assertThat(result).satisfies().is(condition))
        .verifyComplete();
  }

  @Test
  @DisplayName("Checking if an entity exists by their DiscordId is successful")
  public void existsByTheirDiscordId() {
    // given: a discordId
    String discordId = "someDiscordId";
    Mono<UserDetails> entity = Mono.just(UserDetails.builder()
        .discordId(discordId)
        .accessToken("accessToken")
        .refreshToken("refreshToken")
        .discordUsername("discordUsername")
        .build());

    when(reactiveRedisTemplate.opsForValue())
        .thenReturn(reactiveValueOperations);

    when(reactiveValueOperations.get(discordId))
        .thenReturn(entity);

    // when: existsByDiscordId is called
    var response = StepVerifier.create(sut.existsByDiscordId(discordId));

    // then: returns TRUE when it exists in Redis
    response
        .assertNext(result -> Assertions.assertThat(result).isTrue())
        .verifyComplete();
  }

  @Test
  @DisplayName("Checking if an entity exists by their DiscordId fails")
  public void doesNotExistByTheirDiscordId() {
    // given: a discordId
    String discordId = "someDiscordId";

    when(reactiveRedisTemplate.opsForValue())
        .thenReturn(reactiveValueOperations);

    when(reactiveValueOperations.get(discordId))
        .thenReturn(Mono.empty());

    // when: existsByDiscordId is called
    var response = StepVerifier.create(sut.existsByDiscordId(discordId));

    // then: returns FALSE when it does not exist in Redis
    response
        .assertNext(result -> Assertions.assertThat(result).isFalse())
        .verifyComplete();
  }
}
