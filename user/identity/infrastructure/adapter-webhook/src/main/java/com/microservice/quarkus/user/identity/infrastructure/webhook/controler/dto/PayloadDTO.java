package com.microservice.quarkus.user.identity.infrastructure.webhook.controler.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PayloadDTO {
  private String email;
  private String password;
  private UUID clientId;
  private String role;
}
