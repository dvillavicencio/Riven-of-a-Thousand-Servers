package com.danielvm.destiny2bot;

import com.danielvm.destiny2bot.exception.ExternalServiceException;
import com.danielvm.destiny2bot.exception.InternalServerException;
import com.danielvm.destiny2bot.filter.CachingRequestBodyFilter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@EnableCaching
@SpringBootApplication
@EnableAspectJAutoProxy
public class Destiny2botApplication {

  public static void main(String[] args) {
    SpringApplication.run(Destiny2botApplication.class, args);
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  @Bean
  CacheManager inMemoryCacheManager() {
    return new ConcurrentMapCacheManager();
  }

  @Bean
  public FilterRegistrationBean<CachingRequestBodyFilter> signatureValidationFilterBean() {
    FilterRegistrationBean<CachingRequestBodyFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new CachingRequestBodyFilter());
    registrationBean.addUrlPatterns("/interactions");
    return registrationBean;
  }

  /**
   * Prepares a WebClient.Builder bean that has standard status handlers
   *
   * @return {@link WebClient.Builder}
   */
  @Bean
  public WebClient.Builder webClient() {
    return WebClient.builder()
        .defaultStatusHandler(
            HttpStatusCode::is5xxServerError,
            clientResponse -> clientResponse.createException()
                .flatMap(ce -> Mono.error(new ExternalServiceException(
                    ce.getResponseBodyAsString(StandardCharsets.UTF_8),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ce.getCause()))
                )
        )
        .defaultStatusHandler(
            HttpStatusCode::is4xxClientError,
            clientResponse -> clientResponse.createException()
                .flatMap(ce -> Mono.error(
                    new InternalServerException(
                        ce.getResponseBodyAsString(StandardCharsets.UTF_8),
                        HttpStatus.BAD_REQUEST)
                ))
        );
  }

}
