package com.danielvm.destiny2bot.exception;

import org.springframework.http.HttpStatus;

public class ImageProcessingException extends BaseException {

  public ImageProcessingException(String message, Throwable throwable) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR, throwable);
  }
}
