
package com.microservice.quarkus.catalogo.domain.tour.entities.tour;

import com.microservice.quarkus.catalogo.domain.shared.ValueObjectFactory;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

@ApplicationScoped
public class FactoryPriceVO implements ValueObjectFactory<IPriceVO> {
  @Override
  public IPriceVO create(Object... args) {
    BigDecimal amount = (BigDecimal) args[0];
    return new DefaultPriceVO(amount);
  }
}
