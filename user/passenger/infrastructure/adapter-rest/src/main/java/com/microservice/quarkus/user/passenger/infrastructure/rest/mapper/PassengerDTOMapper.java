package com.microservice.quarkus.user.passenger.infrastructure.rest.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.passenger.infrastructure.rest.dto.CompleteRequestDTO;
import com.microservice.quarkus.user.passenger.infrastructure.rest.dto.MeDTO;

@Mapper(componentModel = "cdi")
public interface PassengerDTOMapper {

  @Mapping(target = "id", expression = "java(mapId(passenger))")
  @Mapping(target = "email", expression = "java(mapEmail(passenger))")
  @Mapping(target = "externalId", source = "externalId")
  @Mapping(target = "onboardingCompleted", expression = "java(checkCompletion(passenger))")
  MeDTO toDto(Passenger passenger);

  default UUID mapId(Passenger passenger) {
    return passenger.getId() != null ? UUID.fromString(passenger.getId().value()) : null;
  }

  default String mapEmail(Passenger passenger) {
    return passenger.getEmail() != null ? passenger.getEmail().value() : null;
  }

  default boolean checkCompletion(Passenger passenger) {
    return passenger.getStatus() == PassengerStatus.ACTIVE;
  }

  @Mapping(target = "firstNames", source = "firstNames")
  @Mapping(target = "lastNames", source = "lastNames")
  @Mapping(target = "dni", source = "dni")
  @Mapping(target = "phone", source = "phone")
  CompletePassengerCommand toDomain(CompleteRequestDTO dto);
}
