package com.danielvm.destiny2bot.config;

public interface OAuth2Configuration {

  String getClientId();

  String getClientSecret();

  String getCallbackUrl();

  String getTokenUrl();

  String getAuthorizationUrl();
}
