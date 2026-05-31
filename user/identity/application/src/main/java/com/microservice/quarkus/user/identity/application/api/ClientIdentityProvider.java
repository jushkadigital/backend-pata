package com.microservice.quarkus.user.identity.application.api;

import java.util.List;

import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.TenantConfigDTO;

public interface ClientIdentityProvider {
  String createClient(String name, List<String> items);

  List<ClientSummary> getClientSummaries();

  TenantConfigDTO getTenantConfig(String clientName);

  String getClientNameById(String clientId);
}
