package com.danielvm.destiny2bot.aop;

import com.danielvm.destiny2bot.exception.UserNotAuthorizedException;
import com.danielvm.destiny2bot.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Aspect
@Slf4j
@Component
public class RefreshTokenAdvice {

  private final AuthorizationService authorizationService;

  public RefreshTokenAdvice(
      AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Around("@annotation(com.danielvm.destiny2bot.annotation.RefreshToken) && args(userId)")
  public Mono<Object> refreshToken(ProceedingJoinPoint joinPoint, String userId) {
    return authorizationService.isUserAuthorized(userId)
        .flatMap(authorize -> {
          if (authorize) {
            return Mono.just(true);
          } else {
            String errorMessage = "User with userId [%s] has not granted authorization for this command"
                .formatted(userId);
            log.error(errorMessage);
            return Mono.error(new UserNotAuthorizedException(errorMessage, HttpStatus.BAD_REQUEST));
          }
        })
        .flatMap(consume -> {
          authorizationService.refreshToken(userId);
          try {
            return (Mono<Object>) joinPoint.proceed();
          } catch (Throwable e) {
            return Mono.error(new RuntimeException(e));
          }
        });
  }
}
