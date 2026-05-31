package com.microservice.quarkus.user.identity.application.api;

public interface RealmIdentityProvider {
  String getRealm();

  void configurarWebhook(String url);

  String getToken(String uri);
}
