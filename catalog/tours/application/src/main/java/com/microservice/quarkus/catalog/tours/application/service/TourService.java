package com.microservice.quarkus.catalog.tours.application.service;

import com.microservice.quarkus.catalog.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.catalog.shared.domain.outbox.OutboxEventRepository;
import com.microservice.quarkus.catalog.tours.application.dto.AddPolicyToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceCombinationCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.CreateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.RemoveServiceCommand;
import com.microservice.quarkus.catalog.tours.application.dto.UpdateServiceCommand;
import com.microservice.quarkus.catalog.tours.application.dto.UpdateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.PublishTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.SuspendTourCommand;
import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort;
import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort.CreateProductRequest;
import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort.CreateServiceRequest;
import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort.CreateVariantsRequest;
import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort.VariantData;
import com.microservice.quarkus.catalog.tours.domain.Duration;
import com.microservice.quarkus.catalog.tours.domain.ServiceCombination;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.domain.TourCode;
import com.microservice.quarkus.catalog.tours.domain.TourId;
import com.microservice.quarkus.catalog.tours.domain.TourPolicy;
import com.microservice.quarkus.catalog.tours.domain.TourRepository;
import com.microservice.quarkus.catalog.tours.domain.TourStatus;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceSpec;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceType;
import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TourService {

  private static final String SUBDOMAIN = "Tours";

  @Inject
  OutboxEventRepository outboxEventRepository;

  @Inject
  VendureProductPort vendureProductPort;

  private TourRepository tourRepository;

  @Inject
  public TourService(TourRepository tourRepository) {
    this.tourRepository = tourRepository;
  }

  @Transactional
  public String create(CreateTourCommand cmd) {
    TourCode code = TourCode.of(cmd.code());

    if (tourRepository.existsByCode(code)) {
      throw new IllegalArgumentException("Tour with code " + cmd.code() + " already exists");
    }

    // 1. Create Product in Vendure FIRST
    String slug = cmd.code().toLowerCase().replace("_", "-");
    String vendureProductId = vendureProductPort.createProduct(
        CreateProductRequest.draft(cmd.name(), slug, cmd.description())
    );

    System.out.println("Created Vendure product " + vendureProductId + " for tour " + cmd.code());

    // 2. Create Tour with vendureProductId already assigned
    Duration duration = Duration.of(cmd.durationHours());
    Tour tour = Tour.create(code, cmd.name(), cmd.description(), duration);
    tour.linkToVendureProduct(vendureProductId);

    // Add services if provided (also creates them in Vendure)
    if (cmd.services() != null && !cmd.services().isEmpty()) {
      for (var serviceCmd : cmd.services()) {
        ServiceType serviceType = ServiceType.valueOf(serviceCmd.serviceType());

        // Create service in Vendure
        String serviceSku = generateServiceSku(serviceCmd.serviceType(), serviceCmd.serviceName());
        vendureProductPort.createServiceVariant(
            CreateServiceRequest.of(serviceCmd.serviceType(), serviceCmd.serviceName(), serviceSku)
        );
        System.out.println("Created Vendure service variant for: " + serviceCmd.serviceName() + " (SKU: " + serviceSku + ")");

        ServiceSpec serviceSpec = serviceCmd.isMandatory()
            ? ServiceSpec.mandatory(serviceType, serviceCmd.serviceName())
            : ServiceSpec.optional(serviceType, serviceCmd.serviceName());

        tour.upsertService(serviceSpec, serviceCmd.quantity(), serviceCmd.durationHours());
      }
    }

    tourRepository.save(tour);

    saveToOutbox(tour);

    return tour.getId().value();
  }

  @Transactional
  public void updateTour(UpdateTourCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    Duration duration = null;
    if (cmd.durationHours() != null) {
      duration = Duration.of(cmd.durationHours());
    }

    tour.update(cmd.name(), cmd.description(), duration);
    tourRepository.update(tour);
  }

  @Transactional
  public void addService(AddServiceToTourCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    ServiceType serviceType = ServiceType.valueOf(cmd.serviceType());

    // 1. Create Service in Vendure FIRST
    String serviceSku = generateServiceSku(cmd.serviceType(), cmd.serviceName());
    vendureProductPort.createServiceVariant(
        CreateServiceRequest.of(cmd.serviceType(), cmd.serviceName(), serviceSku)
    );

    System.out.println("Created Vendure service variant for: " + cmd.serviceName() + " (SKU: " + serviceSku + ")");

    // 2. Add service to tour
    ServiceSpec serviceSpec = cmd.isMandatory()
        ? ServiceSpec.mandatory(serviceType, cmd.serviceName())
        : ServiceSpec.optional(serviceType, cmd.serviceName());

    tour.upsertService(serviceSpec, cmd.quantity(), cmd.durationHours());
    tourRepository.update(tour);

    saveToOutbox(tour);
  }

  private String generateServiceSku(String serviceType, String serviceName) {
    String normalizedName = serviceName.toUpperCase()
        .replaceAll("[^A-Z0-9]", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
    return "SVC-" + serviceType.substring(0, Math.min(3, serviceType.length())).toUpperCase()
        + "-" + normalizedName.substring(0, Math.min(20, normalizedName.length()));
  }

  @Transactional
  public void updateService(UpdateServiceCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    // Build the updated ServiceSpec if any field is provided
    ServiceSpec newServiceSpec = null;
    if (cmd.serviceType() != null || cmd.newServiceName() != null) {
      // Find current service to get defaults
      var currentService = tour.getIncludedServices().stream()
          .filter(s -> s.serviceName().equals(cmd.currentServiceName()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Service not found: " + cmd.currentServiceName()));

      ServiceType serviceType = cmd.serviceType() != null
          ? ServiceType.valueOf(cmd.serviceType())
          : currentService.serviceType();

      String serviceName = cmd.newServiceName() != null
          ? cmd.newServiceName()
          : currentService.serviceName();

      boolean mandatory = cmd.isMandatory() != null
          ? cmd.isMandatory()
          : currentService.isMandatory();

      newServiceSpec = mandatory
          ? ServiceSpec.mandatory(serviceType, serviceName)
          : ServiceSpec.optional(serviceType, serviceName);
    }

    tour.updateService(cmd.currentServiceName(), newServiceSpec, cmd.quantity(), cmd.durationHours());
    tourRepository.update(tour);
  }

  @Transactional
  public void removeService(RemoveServiceCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    tour.removeService(cmd.serviceName());
    tourRepository.update(tour);
  }

  @Transactional
  public void addPolicy(AddPolicyToTourCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    TourPolicy.PolicyType policyType = TourPolicy.PolicyType.valueOf(cmd.policyType());
    TourPolicy policy = new TourPolicy(policyType, cmd.description(), cmd.value());
    tour.upsertPolicy(policy);
    tourRepository.update(tour);
  }

  @Transactional
  public void addServiceCombination(AddServiceCombinationCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    if (!tour.isLinkedToVendure()) {
      throw new IllegalStateException("Tour must be linked to Vendure before adding combinations");
    }

    // 1. Calculate price from service prices (sum of draftPrices in Vendure)
    long calculatedPrice = 0L;
    for (String serviceName : cmd.services()) {
      // Find the service in tour to get its type
      var service = tour.getIncludedServices().stream()
          .filter(s -> s.serviceName().equals(serviceName))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Service not found in tour: " + serviceName));

      String serviceSku = generateServiceSku(service.serviceType().name(), serviceName);
      long servicePrice = vendureProductPort.getServicePrice(serviceSku);
      calculatedPrice += servicePrice;

      System.out.println("Service " + serviceName + " (SKU: " + serviceSku + ") price: " + servicePrice);
    }

    // If draftPriceInCents is provided, use it as override, otherwise use calculated price
    long draftPriceInCents = cmd.draftPriceInCents() != null ? cmd.draftPriceInCents() : calculatedPrice;

    System.out.println("Combination " + cmd.sku() + " total price: " + draftPriceInCents + " (calculated: " + calculatedPrice + ")");

    // 2. Create Variant in Vendure
    Map<String, String> variantIds = vendureProductPort.createVariants(
        new CreateVariantsRequest(
            tour.getVendureProductId(),
            List.of(VariantData.draft(cmd.sku(), cmd.name(), draftPriceInCents))
        )
    );

    String vendureVariantId = variantIds.get(cmd.sku());
    if (vendureVariantId == null) {
      throw new IllegalStateException("Failed to get Vendure variant ID for SKU: " + cmd.sku());
    }

    System.out.println("Created Vendure variant " + vendureVariantId + " for combination " + cmd.sku());

    // 3. Create ServiceCombination with vendureVariantId already assigned
    ServiceCombination combination = ServiceCombination.of(
        cmd.name(),
        cmd.description(),
        cmd.sku(),
        cmd.services()
    );

    tour.upsertServiceCombination(combination);

    // 4. Link combination to Vendure variant
    tour.linkCombinationToVendure(cmd.sku(), vendureVariantId, null);

    tourRepository.update(tour);
  }

  @Transactional
  public void publish(PublishTourCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    tour.publish();
    tourRepository.update(tour);

    saveToOutbox(tour);
  }

  @Transactional
  public void suspend(SuspendTourCommand cmd) {
    Tour tour = tourRepository.findById(TourId.of(cmd.tourId()))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + cmd.tourId()));

    tour.suspend(cmd.reason());
    tourRepository.update(tour);

    saveToOutbox(tour);
  }

  @Transactional
  public void resume(String tourId) {
    Tour tour = tourRepository.findById(TourId.of(tourId))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + tourId));

    tour.resume();
    tourRepository.update(tour);
  }

  @Transactional
  public void discontinue(String tourId) {
    Tour tour = tourRepository.findById(TourId.of(tourId))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + tourId));

    tour.discontinue();
    tourRepository.update(tour);

    saveToOutbox(tour);
  }

  public Optional<Tour> findById(String tourId) {
    return tourRepository.findById(TourId.of(tourId));
  }

  public Optional<Tour> findByCode(String code) {
    return tourRepository.findByCode(TourCode.of(code));
  }

  public List<Tour> findAll() {
    return tourRepository.findAll();
  }

  public List<Tour> findByStatus(String status) {
    TourStatus tourStatus = TourStatus.valueOf(status);
    return tourRepository.findByStatus(tourStatus);
  }

  public List<Tour> findPublished() {
    return tourRepository.findPublished();
  }

  public List<Tour> findSellable() {
    return tourRepository.findPublished();
  }

  @Transactional
  public void delete(String tourId) {
    tourRepository.delete(TourId.of(tourId));
  }

  @Transactional
  public String createNewVersion(String tourId) {
    Tour existingTour = tourRepository.findById(TourId.of(tourId))
        .orElseThrow(() -> new IllegalArgumentException("Tour not found: " + tourId));

    Tour newVersion = existingTour.createNewVersion();
    tourRepository.save(newVersion);

    return newVersion.getId().value();
  }

  public List<Tour> findAllVersionsByCode(String code) {
    return tourRepository.findAllVersionsByCode(TourCode.of(code));
  }

  private void saveToOutbox(Tour tour) {
    for (DomainEvent event : tour.domainEvents()) {
      OutboxEvent outboxEvent = OutboxEvent.create(
          SUBDOMAIN,
          "Tour",
          tour.getId().value(),
          event.getClass().getSimpleName(),
          JsonObject.mapFrom(event).encode(),
          event.occurredOn());
      outboxEventRepository.save(outboxEvent);
    }
    tour.clearDomainEvents();
  }
}
