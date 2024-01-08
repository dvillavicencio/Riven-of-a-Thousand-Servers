package com.danielvm.destiny2bot.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {

  @Serial
  private static final long serialVersionUID = -2995183695428792821L;

  public InternalServerException(String message, HttpStatus status) {
    super(message, status);
  }

  public InternalServerException(String message, HttpStatus status, Throwable throwable) {
    super(message, status, throwable);
  }
}
