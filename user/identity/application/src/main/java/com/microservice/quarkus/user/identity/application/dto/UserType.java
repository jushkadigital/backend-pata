package com.microservice.quarkus.user.identity.application.dto;

public enum UserType {
    PASSENGER,
    ADMIN;

    /**
     * Derive user type from a Keycloak composite role name.
     * Composite roles that map to "passengers" group → PASSENGER
     * Composite roles that map to "admins" group → ADMIN
     */
    public static UserType fromCompositeRole(String compositeRole) {
        if (compositeRole == null) {
            return PASSENGER; // default
        }
        return switch (compositeRole) {
            case "basic", "standard", "premium" -> PASSENGER;
            case "editor", "admin", "super-admin" -> ADMIN;
            default -> PASSENGER; // safe default
        };
    }

    /**
     * Derive user type from a set of composite roles.
     * If any role maps to ADMIN → ADMIN, otherwise PASSENGER.
     */
    public static UserType fromCompositeRoles(java.util.Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return PASSENGER;
        }
        for (String role : roles) {
            if (fromCompositeRole(role) == ADMIN) {
                return ADMIN;
            }
        }
        return PASSENGER;
    }
}
