package com.microservice.quarkus.user.iam.infrastructure.webhook.controler.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class KeycloakDTO {
  private UUID id;
  private long time;
  private String type;
  private UUID realmId;
  private UUID clientId;
  private UUID userId;
  private String ipAddress;
  private Details details;
}

@Data
class Details {
  private String authMethod;
  private String authType;
  private String registerMethod;
  private String lastName;
  private String redirectURI;
  private String firstName;
  private UUID codeId;
  private String email;
  private String username;
}
