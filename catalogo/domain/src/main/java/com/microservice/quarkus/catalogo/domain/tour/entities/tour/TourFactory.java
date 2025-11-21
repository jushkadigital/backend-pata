
package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

public class TourFactory implements ITourFactory {
  private final FactoryPriceVO priceFactory;

  // Inyectas las fábricas de los VOs
  public TourFactory(FactoryPriceVO priceFactory) {
    this.priceFactory = priceFactory;
  }

  @Override
  public ITour createTour() {
    // La lógica de la fábrica de Tour se encarga de crear
    // los VOs necesarios y de ensamblar el objeto final.
    IPriceVO price = priceFactory.create(150.0);

    ITour tour = (ITour) Tour.builder().name("gaa").price(price).build();
    return tour;
  }
}
