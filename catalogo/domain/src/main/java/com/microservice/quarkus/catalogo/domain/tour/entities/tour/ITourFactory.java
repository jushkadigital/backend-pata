package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

public interface ITourFactory {
  ITour createTour(); // No recibe parámetros, los VOs se crean internamente.
}
