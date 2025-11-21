
package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

import java.math.BigDecimal;

public interface IPriceVO {
  BigDecimal getFinalValue();

  default BigDecimal getDiscountAmount() {
    return BigDecimal.ZERO;
  }

  default void validate() {
  }
}
