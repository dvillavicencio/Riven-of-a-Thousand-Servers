package com.danielvm.destiny2bot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods with this annotation are marked so that the application will automatically check for
 * access token refresh based on the current entry in Redis and if the current access token saved is
 * expired, it will fetch another one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface RefreshToken {

}
