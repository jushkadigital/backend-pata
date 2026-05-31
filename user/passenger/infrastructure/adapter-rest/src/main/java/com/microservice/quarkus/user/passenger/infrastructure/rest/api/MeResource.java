package com.microservice.quarkus.user.passenger.infrastructure.rest.api;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.infrastructure.rest.dto.CompleteRequestDTO;
import com.microservice.quarkus.user.passenger.infrastructure.rest.mapper.PassengerDTOMapper;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MeResource implements MeAPI {
  @Inject
  PassengerRepository repository;
  @Inject
  PassengerService passengerService;
  @Inject
  PassengerDTOMapper userMapper;
  @Inject
  SecurityIdentity securityIdentity;
  @Inject
  JsonWebToken jsonWebToken;

  @Override
  public Response getPassengerInfo() {
    Response response;

    response = Response.noContent().build();

    User user = new User(securityIdentity, jsonWebToken);

    Passenger passenger = repository.findByExternalId(user.id).orElse(null);

    if (null == passenger) {
      response = Response.status(Response.Status.NOT_FOUND).build();
    } else {
      response = Response.ok(userMapper.toDto(passenger)).build();
    }

    return response;
  }

  @Override
  public Response completePassengerProfile(CompleteRequestDTO completeRequestDTO) {
    User user = new User(securityIdentity, jsonWebToken);

    Passenger passenger = repository.findByExternalId(user.id).orElse(null);

    if (null == passenger) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    CompletePassengerCommand cmd = new CompletePassengerCommand(completeRequestDTO.getFirstNames(),
        completeRequestDTO.getLastNames(), completeRequestDTO.getDni(), completeRequestDTO.getPhone());

    passengerService.complete(user.id, cmd);

    return Response.ok().build();
  }

  public static class User {

    private final String userName;
    private final String id;

    User(SecurityIdentity securityIdentity, JsonWebToken jsonWebToken) {
      this.userName = securityIdentity.getPrincipal().getName();
      this.id = jsonWebToken.getSubject();
    }

    public String getUserName() {
      return userName;
    }

    public String getId() {
      return id;
    }
  }

}
