package com.microservice.quarkus.catalog.tours.domain;

import java.math.BigDecimal;
import java.util.Currency;

public record BasePrice(BigDecimal amount, Currency currency) {

  public BasePrice {
    if (amount == null) {
      throw new IllegalArgumentException("Price amount cannot be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Price amount cannot be negative");
    }
    if (currency == null) {
      throw new IllegalArgumentException("Currency cannot be null");
    }
  }

  public static BasePrice of(BigDecimal amount, String currencyCode) {
    return new BasePrice(amount, Currency.getInstance(currencyCode));
  }

  public static BasePrice of(double amount, String currencyCode) {
    return new BasePrice(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
  }

  public static BasePrice zero(String currencyCode) {
    return new BasePrice(BigDecimal.ZERO, Currency.getInstance(currencyCode));
  }

  public boolean isZero() {
    return amount.compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isGreaterThan(BasePrice other) {
    validateSameCurrency(other);
    return this.amount.compareTo(other.amount) > 0;
  }

  public BasePrice add(BasePrice other) {
    validateSameCurrency(other);
    return new BasePrice(this.amount.add(other.amount), this.currency);
  }

  private void validateSameCurrency(BasePrice other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException("Cannot compare prices with different currencies");
    }
  }

  @Override
  public String toString() {
    return amount.toPlainString() + " " + currency.getCurrencyCode();
  }
}
