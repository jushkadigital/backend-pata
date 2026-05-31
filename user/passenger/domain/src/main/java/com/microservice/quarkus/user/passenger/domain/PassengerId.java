package com.microservice.quarkus.user.passenger.domain;

import java.util.UUID;

public record PassengerId(String value) {

    public PassengerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El PassengerId no puede ser nulo ni estar vacío.");
        }
    }

    public static PassengerId random() {
        return new PassengerId(UUID.randomUUID().toString());
    }

    public static PassengerId of(String value) {
        return new PassengerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
