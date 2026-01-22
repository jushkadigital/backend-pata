package com.microservice.quarkus.catalog.tours.domain.service;

public record FoodConfiguration(
    String mealType,
    boolean includesDrinks,
    String dietaryRestrictions
) implements ServiceConfiguration {

    public FoodConfiguration {
        if (mealType == null || mealType.isBlank()) {
            throw new IllegalArgumentException("Meal type cannot be null or blank");
        }
    }

    public static FoodConfiguration breakfast(boolean includesDrinks) {
        return new FoodConfiguration("BREAKFAST", includesDrinks, null);
    }

    public static FoodConfiguration lunch(boolean includesDrinks) {
        return new FoodConfiguration("LUNCH", includesDrinks, null);
    }

    public static FoodConfiguration dinner(boolean includesDrinks) {
        return new FoodConfiguration("DINNER", includesDrinks, null);
    }

    public static FoodConfiguration fullBoard() {
        return new FoodConfiguration("FULL_BOARD", true, null);
    }

    public FoodConfiguration withDietaryRestrictions(String restrictions) {
        return new FoodConfiguration(this.mealType, this.includesDrinks, restrictions);
    }
}
