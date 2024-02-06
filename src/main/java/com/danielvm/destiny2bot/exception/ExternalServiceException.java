package com.danielvm.destiny2bot.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BaseException {

  @Serial
  private static final long serialVersionUID = -4491949753852333353L;

  public ExternalServiceException(String message, HttpStatus status, Throwable cause) {
    super(message, status, cause);
  }

  public ExternalServiceException(String message) {
    super(message, HttpStatus.SERVICE_UNAVAILABLE);
  }
}
