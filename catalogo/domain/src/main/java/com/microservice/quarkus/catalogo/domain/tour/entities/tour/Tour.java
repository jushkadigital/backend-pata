package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

import com.microservice.quarkus.catalogo.domain.shared.Entity;
import com.microservice.quarkus.catalogo.domain.shared.RootAggregate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
class Tour extends RootAggregate implements Entity<Tour> {
  private TourId id;
  private String name;
  private IPriceVO price;

  @Override
  public boolean sameIdentityAs(Tour other) {
    return other != null && this.id.sameValueAs(other.getId());
  }
}
