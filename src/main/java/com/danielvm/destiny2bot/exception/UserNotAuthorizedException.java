package com.danielvm.destiny2bot.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;

public class UserNotAuthorizedException extends BaseException {

  @Serial
  private static final long serialVersionUID = -2336112387889226L;

  public UserNotAuthorizedException(String message, HttpStatus status) {
    super(message, status);
  }
}
