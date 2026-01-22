package com.microservice.quarkus.catalog.tours.domain;

import com.microservice.quarkus.catalog.tours.domain.events.CombinationPriceUpdatedEvent;
import com.microservice.quarkus.catalog.tours.domain.events.ServiceAddedToTourEvent;
import com.microservice.quarkus.catalog.tours.domain.events.TourCreatedEvent;
import com.microservice.quarkus.catalog.tours.domain.events.TourDiscontinuedEvent;
import com.microservice.quarkus.catalog.tours.domain.events.TourPublishedEvent;
import com.microservice.quarkus.catalog.tours.domain.events.TourSuspendedEvent;
import com.microservice.quarkus.catalog.tours.domain.events.TourUpdatedEvent;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceSpec;
import com.microservice.quarkus.catalog.tours.domain.shared.RootAggregate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tour extends RootAggregate {

    private TourId id;
    private TourCode code;
    private String name;
    private String description;
    private Duration duration;
    @Builder.Default
    private List<IncludedService> includedServices = new ArrayList<>();
    @Builder.Default
    private List<TourPolicy> policies = new ArrayList<>();
    @Builder.Default
    private List<ServiceCombination> serviceCombinations = new ArrayList<>();
    private TourStatus status;
    @Builder.Default
    private int version = 1;
    private TourId parentTourId;
    private Instant createdAt;
    private Instant updatedAt;

    // Vendure integration
    private String vendureProductId;

    public static Tour create(
            TourCode code,
            String name,
            String description,
            Duration duration) {

        Tour tour = Tour.builder()
                .id(TourId.newId())
                .code(code)
                .name(name)
                .description(description)
                .duration(duration)
                .includedServices(new ArrayList<>())
                .policies(new ArrayList<>())
                .serviceCombinations(new ArrayList<>())
                .status(TourStatus.DRAFT)
                .version(1)
                .parentTourId(null)
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();

        tour.registerEvent(new TourCreatedEvent(
                tour.id.value(),
                tour.code.value(),
                tour.name,
                tour.description,
                tour.duration.toString(),
                0));

        return tour;
    }

    public void addService(ServiceSpec service, int quantity, Integer durationHours) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify services on a non-draft tour");
        }

        boolean alreadyIncluded = includedServices.stream()
                .anyMatch(s -> s.serviceName().equals(service.name()));
        if (alreadyIncluded) {
            throw new IllegalArgumentException("Service already included in this tour");
        }

        IncludedService includedService = IncludedService.of(service, quantity, durationHours);
        this.includedServices.add(includedService);
        this.updatedAt = Instant.now();

        registerEvent(new ServiceAddedToTourEvent(
                this.id.value(),
                service.name(),
                service.type().name(),
                service.mandatory()));
    }

    public void addService(ServiceSpec service, int quantity) {
        addService(service, quantity, null);
    }

    public void addService(ServiceSpec service) {
        addService(service, 1, null);
    }

    public boolean upsertService(ServiceSpec service, int quantity, Integer durationHours) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify services on a non-draft tour");
        }

        int existingIndex = -1;
        for (int i = 0; i < includedServices.size(); i++) {
            if (includedServices.get(i).serviceName().equals(service.name())) {
                existingIndex = i;
                break;
            }
        }

        IncludedService includedService = IncludedService.of(service, quantity, durationHours);

        if (existingIndex >= 0) {
            includedServices.set(existingIndex, includedService);
            this.updatedAt = Instant.now();
            return false;
        } else {
            this.includedServices.add(includedService);
            this.updatedAt = Instant.now();

            registerEvent(new ServiceAddedToTourEvent(
                    this.id.value(),
                    service.name(),
                    service.type().name(),
                    service.mandatory()));
            return true;
        }
    }

    public void updateService(String serviceName, ServiceSpec newService, Integer quantity, Integer durationHours) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify services on a non-draft tour");
        }

        int serviceIndex = -1;
        for (int i = 0; i < includedServices.size(); i++) {
            if (includedServices.get(i).serviceName().equals(serviceName)) {
                serviceIndex = i;
                break;
            }
        }

        if (serviceIndex == -1) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }

        IncludedService currentService = includedServices.get(serviceIndex);

        if (newService != null && !newService.name().equals(serviceName)) {
            boolean nameConflict = includedServices.stream()
                    .anyMatch(s -> s.serviceName().equals(newService.name()));
            if (nameConflict) {
                throw new IllegalArgumentException("A service with name '" + newService.name() + "' already exists");
            }
        }

        ServiceSpec updatedSpec = newService != null ? newService : currentService.service();
        int updatedQuantity = quantity != null ? quantity : currentService.quantity();
        Integer updatedDuration = durationHours != null ? durationHours : currentService.durationHours();

        IncludedService updatedService = IncludedService.of(updatedSpec, updatedQuantity, updatedDuration);
        includedServices.set(serviceIndex, updatedService);
        this.updatedAt = Instant.now();
    }

    public void removeService(String serviceName) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify services on a non-draft tour");
        }

        boolean exists = includedServices.stream()
                .anyMatch(s -> s.serviceName().equals(serviceName));
        if (!exists) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }

        for (ServiceCombination combination : serviceCombinations) {
            if (combination.containsService(serviceName)) {
                throw new IllegalStateException(
                    "Cannot remove service '" + serviceName + "' because it is used in combination '" + combination.name() + "'"
                );
            }
        }

        includedServices.removeIf(s -> s.serviceName().equals(serviceName));
        this.updatedAt = Instant.now();
    }

    public void addPolicy(TourPolicy policy) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify policies on a non-draft tour");
        }

        boolean alreadyExists = policies.stream()
                .anyMatch(p -> p.type() == policy.type());
        if (alreadyExists) {
            throw new IllegalArgumentException("Policy of this type already exists");
        }

        this.policies.add(policy);
        this.updatedAt = Instant.now();
    }

    public boolean upsertPolicy(TourPolicy policy) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify policies on a non-draft tour");
        }

        int existingIndex = -1;
        for (int i = 0; i < policies.size(); i++) {
            if (policies.get(i).type() == policy.type()) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            policies.set(existingIndex, policy);
            this.updatedAt = Instant.now();
            return false;
        } else {
            this.policies.add(policy);
            this.updatedAt = Instant.now();
            return true;
        }
    }

    public void addServiceCombination(ServiceCombination combination) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify service combinations on a non-draft tour");
        }

        boolean nameExists = serviceCombinations.stream()
                .anyMatch(c -> c.name().equals(combination.name()));
        if (nameExists) {
            throw new IllegalArgumentException("A combination with this name already exists");
        }

        boolean skuExists = serviceCombinations.stream()
                .anyMatch(c -> c.sku().equals(combination.sku()));
        if (skuExists) {
            throw new IllegalArgumentException("A combination with this SKU already exists");
        }

        for (String serviceName : combination.getServiceNames()) {
            boolean serviceExists = includedServices.stream()
                    .anyMatch(s -> s.serviceName().equals(serviceName));
            if (!serviceExists) {
                throw new IllegalArgumentException("Service '" + serviceName + "' is not included in this tour");
            }
        }

        this.serviceCombinations.add(combination);
        this.updatedAt = Instant.now();

        emitCombinationsUpdatedEvent();
    }

    public boolean upsertServiceCombination(ServiceCombination combination) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify service combinations on a non-draft tour");
        }

        for (String serviceName : combination.getServiceNames()) {
            boolean serviceExists = includedServices.stream()
                    .anyMatch(s -> s.serviceName().equals(serviceName));
            if (!serviceExists) {
                throw new IllegalArgumentException("Service '" + serviceName + "' is not included in this tour");
            }
        }

        int existingIndex = -1;
        for (int i = 0; i < serviceCombinations.size(); i++) {
            if (serviceCombinations.get(i).name().equals(combination.name())) {
                existingIndex = i;
                break;
            }
        }

        boolean isNew;
        if (existingIndex >= 0) {
            serviceCombinations.set(existingIndex, combination);
            this.updatedAt = Instant.now();
            isNew = false;
        } else {
            // Check SKU uniqueness for new combinations
            boolean skuExists = serviceCombinations.stream()
                    .anyMatch(c -> c.sku().equals(combination.sku()));
            if (skuExists) {
                throw new IllegalArgumentException("A combination with this SKU already exists");
            }
            this.serviceCombinations.add(combination);
            this.updatedAt = Instant.now();
            isNew = true;
        }

        emitCombinationsUpdatedEvent();
        return isNew;
    }

    public void update(String name, String description, Duration duration) {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT tours can be updated");
        }

        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (duration != null) {
            this.duration = duration;
        }

        this.updatedAt = Instant.now();

        emitCombinationsUpdatedEvent();
    }

    public void publish() {
        if (this.status != TourStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT tours can be published");
        }

        validatePublishingRules();

        this.status = TourStatus.PUBLISHED;
        this.updatedAt = Instant.now();

        List<TourPublishedEvent.CombinationData> combinationDataList = this.serviceCombinations.stream()
                .map(combo -> new TourPublishedEvent.CombinationData(
                        combo.sku(),
                        combo.name(),
                        combo.description()
                ))
                .toList();

        registerEvent(new TourPublishedEvent(
                this.id.value(),
                this.code.value(),
                this.name,
                this.description,
                combinationDataList));
    }

    public void suspend(String reason) {
        if (this.status != TourStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED tours can be suspended");
        }

        this.status = TourStatus.SUSPENDED;
        this.updatedAt = Instant.now();

        registerEvent(new TourSuspendedEvent(
                this.id.value(),
                this.code.value(),
                reason));
    }

    public void resume() {
        if (this.status != TourStatus.SUSPENDED) {
            throw new IllegalStateException("Only SUSPENDED tours can be resumed");
        }

        this.status = TourStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void discontinue() {
        if (this.status == TourStatus.DISCONTINUED) {
            throw new IllegalStateException("Tour is already discontinued");
        }

        this.status = TourStatus.DISCONTINUED;
        this.updatedAt = Instant.now();

        registerEvent(new TourDiscontinuedEvent(
                this.id.value(),
                this.code.value()));
    }

    public Tour createNewVersion() {
        List<IncludedService> copiedServices = new ArrayList<>();
        for (IncludedService service : this.includedServices) {
            copiedServices.add(IncludedService.of(
                    service.service(),
                    service.quantity(),
                    service.durationHours()
            ));
        }

        List<TourPolicy> copiedPolicies = new ArrayList<>(this.policies);

        List<ServiceCombination> copiedCombinations = new ArrayList<>();
        for (ServiceCombination combination : this.serviceCombinations) {
            copiedCombinations.add(new ServiceCombination(
                    combination.name(),
                    combination.description(),
                    combination.sku(),
                    new ArrayList<>(combination.items()),
                    combination.vendureVariantId(),
                    combination.price()
            ));
        }

        Tour newVersion = Tour.builder()
                .id(TourId.newId())
                .code(this.code)
                .name(this.name)
                .description(this.description)
                .duration(this.duration)
                .includedServices(copiedServices)
                .policies(copiedPolicies)
                .serviceCombinations(copiedCombinations)
                .status(TourStatus.DRAFT)
                .version(this.version + 1)
                .parentTourId(this.id)
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();

        return newVersion;
    }

    public boolean canBeSold() {
        return this.status == TourStatus.PUBLISHED;
    }

    public boolean canBePublished() {
        if (this.status != TourStatus.DRAFT) {
            return false;
        }
        return hasAtLeastOneCombination();
    }

    public boolean hasAtLeastOneCombination() {
        return serviceCombinations != null && !serviceCombinations.isEmpty();
    }

    public boolean hasAtLeastOneService() {
        return includedServices != null && !includedServices.isEmpty();
    }

    public int getMandatoryServicesCount() {
        return (int) includedServices.stream()
                .filter(IncludedService::isMandatory)
                .count();
    }

    public int getOptionalServicesCount() {
        return (int) includedServices.stream()
                .filter(s -> !s.isMandatory())
                .count();
    }

    public List<IncludedService> getIncludedServices() {
        return Collections.unmodifiableList(includedServices);
    }

    public List<TourPolicy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    public List<ServiceCombination> getServiceCombinations() {
        return Collections.unmodifiableList(serviceCombinations);
    }

    private void validatePublishingRules() {
        if (!hasAtLeastOneCombination()) {
            throw new IllegalStateException("Tour must have at least one service combination to be published");
        }
    }

    private void emitCombinationsUpdatedEvent() {
        List<TourUpdatedEvent.CombinationData> combinationDataList = this.serviceCombinations.stream()
                .map(combo -> new TourUpdatedEvent.CombinationData(
                        combo.sku(),
                        combo.name(),
                        combo.description()
                ))
                .toList();

        registerEvent(new TourUpdatedEvent(
                this.id.value(),
                this.code.value(),
                this.name,
                this.description,
                this.duration.toString(),
                combinationDataList));
    }

    // ========== Vendure Integration ==========

    /**
     * Links this tour to a Vendure product.
     */
    public void linkToVendureProduct(String vendureProductId) {
        this.vendureProductId = vendureProductId;
        this.updatedAt = Instant.now();
    }

    /**
     * Links a combination to a Vendure variant and sets its price.
     */
    public void linkCombinationToVendure(String sku, String vendureVariantId, Money price) {
        for (int i = 0; i < serviceCombinations.size(); i++) {
            ServiceCombination combo = serviceCombinations.get(i);
            if (combo.sku().equals(sku)) {
                serviceCombinations.set(i, combo.withVendureLink(vendureVariantId, price));
                this.updatedAt = Instant.now();
                return;
            }
        }
        throw new IllegalArgumentException("Combination with SKU '" + sku + "' not found");
    }

    /**
     * Updates the draft price of a combination and emits event for Vendure sync.
     */
    public void updateCombinationDraftPrice(String sku, Money price) {
        for (int i = 0; i < serviceCombinations.size(); i++) {
            ServiceCombination combo = serviceCombinations.get(i);
            if (combo.sku().equals(sku)) {
                serviceCombinations.set(i, combo.withPrice(price));
                this.updatedAt = Instant.now();

                // Emit event for Vendure DraftPrice update if linked
                if (combo.isLinkedToVendure()) {
                    registerEvent(new CombinationPriceUpdatedEvent(
                            this.id.value(),
                            this.code.value(),
                            sku,
                            combo.vendureVariantId(),
                            price.amount(),
                            price.currency()
                    ));
                }
                return;
            }
        }
        throw new IllegalArgumentException("Combination with SKU '" + sku + "' not found");
    }

    public boolean isLinkedToVendure() {
        return vendureProductId != null && !vendureProductId.isBlank();
    }
}
