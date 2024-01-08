package com.danielvm.destiny2bot.config;

import com.danielvm.destiny2bot.entity.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
public class RedisConfiguration {

  @Bean
  public ReactiveRedisTemplate<String, UserDetails> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    Jackson2JsonRedisSerializer<UserDetails> valueSerializer = new Jackson2JsonRedisSerializer<>(
        objectMapper, UserDetails.class);
    RedisSerializationContext.RedisSerializationContextBuilder<String, UserDetails> builder =
        RedisSerializationContext.newSerializationContext(keySerializer);
    RedisSerializationContext<String, UserDetails> context =
        builder.value(valueSerializer).build();
    return new ReactiveRedisTemplate<>(redisConnectionFactory, context);
  }
}
