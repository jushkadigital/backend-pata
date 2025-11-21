package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

import com.microservice.quarkus.catalogo.domain.shared.ValueObject;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TourId implements ValueObject<TourId> {

  private String id;

  @Override
  public boolean sameValueAs(TourId other) {
    return other != null && this.id.equals(other.id);
  }

}
