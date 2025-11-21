
package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

import java.math.BigDecimal;

import com.microservice.quarkus.catalogo.domain.shared.ValueObject;

public class DefaultPriceVO implements IPriceVO, ValueObject<DefaultPriceVO> {
  private final BigDecimal amount;

  public DefaultPriceVO(BigDecimal amount) {
    this.amount = amount;
    validate();
  }

  @Override
  public BigDecimal getFinalValue() {
    return amount;
  }

  @Override
  public boolean sameValueAs(DefaultPriceVO other) {
    return this.amount == other.amount;
  }
}
