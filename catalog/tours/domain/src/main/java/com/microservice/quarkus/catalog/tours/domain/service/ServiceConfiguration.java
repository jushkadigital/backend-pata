package com.microservice.quarkus.catalog.tours.domain.service;

public sealed interface ServiceConfiguration
    permits TicketConfiguration, FoodConfiguration, TransportConfiguration {
}
