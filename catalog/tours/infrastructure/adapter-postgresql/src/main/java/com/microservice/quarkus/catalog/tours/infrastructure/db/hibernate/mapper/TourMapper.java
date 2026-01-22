package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.catalog.tours.domain.Duration;
import com.microservice.quarkus.catalog.tours.domain.IncludedService;
import com.microservice.quarkus.catalog.tours.domain.Money;
import com.microservice.quarkus.catalog.tours.domain.ServiceCombination;
import com.microservice.quarkus.catalog.tours.domain.ServiceCombinationItem;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.domain.TourCode;
import com.microservice.quarkus.catalog.tours.domain.TourId;
import com.microservice.quarkus.catalog.tours.domain.TourPolicy;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceSpec;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceType;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.IncludedServiceEntity;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.ServiceCombinationEntity;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.ServiceCombinationItemEntity;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.TourEntity;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.TourPolicyEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface TourMapper {

    @Mapping(target = "id", expression = "java(domain.getId().value())")
    @Mapping(target = "code", expression = "java(domain.getCode().value())")
    @Mapping(target = "durationHours", expression = "java(domain.getDuration().hours())")
    @Mapping(target = "version", expression = "java(domain.getVersion())")
    @Mapping(target = "parentTourId", expression = "java(domain.getParentTourId() != null ? domain.getParentTourId().value() : null)")
    @Mapping(target = "includedServices", ignore = true)
    @Mapping(target = "policies", ignore = true)
    @Mapping(target = "serviceCombinations", ignore = true)
    TourEntity toDbo(Tour domain);

    @AfterMapping
    default void mapIncludedServices(Tour domain, @MappingTarget TourEntity entity) {
        List<IncludedServiceEntity> serviceEntities = new ArrayList<>();
        for (IncludedService service : domain.getIncludedServices()) {
            IncludedServiceEntity serviceEntity = new IncludedServiceEntity();
            serviceEntity.setTour(entity);
            serviceEntity.setServiceType(service.serviceType().name());
            serviceEntity.setServiceName(service.serviceName());
            serviceEntity.setQuantity(service.quantity());
            serviceEntity.setMandatory(service.isMandatory());
            serviceEntity.setDurationHours(service.durationHours());
            serviceEntities.add(serviceEntity);
        }
        entity.setIncludedServices(serviceEntities);
    }

    @AfterMapping
    default void mapPolicies(Tour domain, @MappingTarget TourEntity entity) {
        List<TourPolicyEntity> policyEntities = new ArrayList<>();
        for (TourPolicy policy : domain.getPolicies()) {
            TourPolicyEntity policyEntity = new TourPolicyEntity();
            policyEntity.setTour(entity);
            policyEntity.setPolicyType(policy.type());
            policyEntity.setDescription(policy.description());
            policyEntity.setValue(policy.value());
            policyEntities.add(policyEntity);
        }
        entity.setPolicies(policyEntities);
    }

    @AfterMapping
    default void mapServiceCombinations(Tour domain, @MappingTarget TourEntity entity) {
        entity.setVendureProductId(domain.getVendureProductId());

        List<ServiceCombinationEntity> combinationEntities = new ArrayList<>();
        int displayOrder = 0;
        for (ServiceCombination combination : domain.getServiceCombinations()) {
            ServiceCombinationEntity combinationEntity = new ServiceCombinationEntity();
            combinationEntity.setTour(entity);
            combinationEntity.setName(combination.name());
            combinationEntity.setDescription(combination.description());
            combinationEntity.setSku(combination.sku());
            combinationEntity.setDisplayOrder(displayOrder++);
            combinationEntity.setVendureVariantId(combination.vendureVariantId());
            if (combination.price() != null) {
                combinationEntity.setPriceAmount(combination.price().amount());
                combinationEntity.setPriceCurrency(combination.price().currency());
            }

            List<ServiceCombinationItemEntity> itemEntities = new ArrayList<>();
            for (ServiceCombinationItem item : combination.items()) {
                ServiceCombinationItemEntity itemEntity = new ServiceCombinationItemEntity();
                itemEntity.setCombination(combinationEntity);
                itemEntity.setServiceName(item.serviceName());
                itemEntity.setItemOrder(item.order());
                itemEntities.add(itemEntity);
            }
            combinationEntity.setItems(itemEntities);
            combinationEntities.add(combinationEntity);
        }
        entity.setServiceCombinations(combinationEntities);
    }

    default Tour toDomain(TourEntity entity) {
        if (entity == null) {
            return null;
        }

        List<IncludedService> includedServices = new ArrayList<>();
        if (entity.getIncludedServices() != null) {
            for (IncludedServiceEntity serviceEntity : entity.getIncludedServices()) {
                ServiceType serviceType = ServiceType.valueOf(serviceEntity.getServiceType());
                ServiceSpec serviceSpec = serviceEntity.isMandatory()
                        ? ServiceSpec.mandatory(serviceType, serviceEntity.getServiceName())
                        : ServiceSpec.optional(serviceType, serviceEntity.getServiceName());
                IncludedService service = IncludedService.of(serviceSpec, serviceEntity.getQuantity(), serviceEntity.getDurationHours());
                includedServices.add(service);
            }
        }

        List<TourPolicy> policies = new ArrayList<>();
        if (entity.getPolicies() != null) {
            for (TourPolicyEntity policyEntity : entity.getPolicies()) {
                TourPolicy policy = new TourPolicy(
                        policyEntity.getPolicyType(),
                        policyEntity.getDescription(),
                        policyEntity.getValue()
                );
                policies.add(policy);
            }
        }

        List<ServiceCombination> serviceCombinations = new ArrayList<>();
        if (entity.getServiceCombinations() != null) {
            for (ServiceCombinationEntity combinationEntity : entity.getServiceCombinations()) {
                List<ServiceCombinationItem> items = new ArrayList<>();
                if (combinationEntity.getItems() != null) {
                    for (ServiceCombinationItemEntity itemEntity : combinationEntity.getItems()) {
                        items.add(ServiceCombinationItem.of(
                                itemEntity.getServiceName(),
                                itemEntity.getItemOrder()
                        ));
                    }
                }
                Money price = null;
                if (combinationEntity.getPriceAmount() != null && combinationEntity.getPriceCurrency() != null) {
                    price = Money.of(combinationEntity.getPriceAmount(), combinationEntity.getPriceCurrency());
                }
                serviceCombinations.add(new ServiceCombination(
                        combinationEntity.getName(),
                        combinationEntity.getDescription(),
                        combinationEntity.getSku(),
                        items,
                        combinationEntity.getVendureVariantId(),
                        price
                ));
            }
        }

        return Tour.builder()
                .id(TourId.of(entity.getId()))
                .code(TourCode.of(entity.getCode()))
                .name(entity.getName())
                .description(entity.getDescription())
                .duration(Duration.of(entity.getDurationHours()))
                .includedServices(includedServices)
                .policies(policies)
                .serviceCombinations(serviceCombinations)
                .status(entity.getStatus())
                .version(entity.getVersion())
                .parentTourId(entity.getParentTourId() != null ? TourId.of(entity.getParentTourId()) : null)
                .vendureProductId(entity.getVendureProductId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    default void updateEntityFromDomain(Tour domain, @MappingTarget TourEntity entity) {
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setDurationHours(domain.getDuration().hours());
        entity.setStatus(domain.getStatus());
        entity.setVersion(domain.getVersion());
        entity.setParentTourId(domain.getParentTourId() != null ? domain.getParentTourId().value() : null);
        entity.setVendureProductId(domain.getVendureProductId());
        entity.setUpdatedAt(domain.getUpdatedAt());

        // Update included services - update existing, remove deleted, add new
        updateIncludedServices(domain, entity);

        // Update policies - update existing, remove deleted, add new
        updatePolicies(domain, entity);

        // Update service combinations - update existing, remove deleted, add new
        updateServiceCombinations(domain, entity);
    }

    default void updateIncludedServices(Tour domain, TourEntity entity) {
        // Create map of existing services by name
        Map<String, IncludedServiceEntity> existingByName = entity.getIncludedServices().stream()
                .collect(Collectors.toMap(IncludedServiceEntity::getServiceName, s -> s));

        // Get names from domain
        Set<String> domainServiceNames = domain.getIncludedServices().stream()
                .map(IncludedService::serviceName)
                .collect(Collectors.toSet());

        // Remove services no longer in domain
        entity.getIncludedServices().removeIf(s -> !domainServiceNames.contains(s.getServiceName()));

        // Update existing or add new
        for (IncludedService service : domain.getIncludedServices()) {
            IncludedServiceEntity existing = existingByName.get(service.serviceName());
            if (existing != null) {
                // Update existing
                existing.setServiceType(service.serviceType().name());
                existing.setQuantity(service.quantity());
                existing.setMandatory(service.isMandatory());
                existing.setDurationHours(service.durationHours());
            } else {
                // Add new
                IncludedServiceEntity newEntity = new IncludedServiceEntity();
                newEntity.setTour(entity);
                newEntity.setServiceType(service.serviceType().name());
                newEntity.setServiceName(service.serviceName());
                newEntity.setQuantity(service.quantity());
                newEntity.setMandatory(service.isMandatory());
                newEntity.setDurationHours(service.durationHours());
                entity.getIncludedServices().add(newEntity);
            }
        }
    }

    default void updatePolicies(Tour domain, TourEntity entity) {
        // Create map of existing policies by type
        Map<TourPolicy.PolicyType, TourPolicyEntity> existingByType = entity.getPolicies().stream()
                .collect(Collectors.toMap(TourPolicyEntity::getPolicyType, p -> p));

        // Get types from domain
        Set<TourPolicy.PolicyType> domainPolicyTypes = domain.getPolicies().stream()
                .map(TourPolicy::type)
                .collect(Collectors.toSet());

        // Remove policies no longer in domain
        entity.getPolicies().removeIf(p -> !domainPolicyTypes.contains(p.getPolicyType()));

        // Update existing or add new
        for (TourPolicy policy : domain.getPolicies()) {
            TourPolicyEntity existing = existingByType.get(policy.type());
            if (existing != null) {
                // Update existing
                existing.setDescription(policy.description());
                existing.setValue(policy.value());
            } else {
                // Add new
                TourPolicyEntity newEntity = new TourPolicyEntity();
                newEntity.setTour(entity);
                newEntity.setPolicyType(policy.type());
                newEntity.setDescription(policy.description());
                newEntity.setValue(policy.value());
                entity.getPolicies().add(newEntity);
            }
        }
    }

    default void updateServiceCombinations(Tour domain, TourEntity entity) {
        // Create map of existing combinations by name
        Map<String, ServiceCombinationEntity> existingByName = entity.getServiceCombinations().stream()
                .collect(Collectors.toMap(ServiceCombinationEntity::getName, c -> c));

        // Get names from domain
        Set<String> domainCombinationNames = domain.getServiceCombinations().stream()
                .map(ServiceCombination::name)
                .collect(Collectors.toSet());

        // Remove combinations no longer in domain
        entity.getServiceCombinations().removeIf(c -> !domainCombinationNames.contains(c.getName()));

        // Update existing or add new
        int displayOrder = 0;
        for (ServiceCombination combination : domain.getServiceCombinations()) {
            ServiceCombinationEntity existing = existingByName.get(combination.name());
            if (existing != null) {
                // Update existing
                existing.setDescription(combination.description());
                existing.setSku(combination.sku());
                existing.setDisplayOrder(displayOrder++);
                existing.setVendureVariantId(combination.vendureVariantId());
                if (combination.price() != null) {
                    existing.setPriceAmount(combination.price().amount());
                    existing.setPriceCurrency(combination.price().currency());
                }

                // Update items properly - don't use clear()
                updateCombinationItems(combination, existing);
            } else {
                // Add new
                ServiceCombinationEntity newEntity = new ServiceCombinationEntity();
                newEntity.setTour(entity);
                newEntity.setName(combination.name());
                newEntity.setDescription(combination.description());
                newEntity.setSku(combination.sku());
                newEntity.setDisplayOrder(displayOrder++);
                newEntity.setVendureVariantId(combination.vendureVariantId());
                if (combination.price() != null) {
                    newEntity.setPriceAmount(combination.price().amount());
                    newEntity.setPriceCurrency(combination.price().currency());
                }

                List<ServiceCombinationItemEntity> itemEntities = new ArrayList<>();
                for (ServiceCombinationItem item : combination.items()) {
                    ServiceCombinationItemEntity itemEntity = new ServiceCombinationItemEntity();
                    itemEntity.setCombination(newEntity);
                    itemEntity.setServiceName(item.serviceName());
                    itemEntity.setItemOrder(item.order());
                    itemEntities.add(itemEntity);
                }
                newEntity.setItems(itemEntities);
                entity.getServiceCombinations().add(newEntity);
            }
        }
    }

    default void updateCombinationItems(ServiceCombination domain, ServiceCombinationEntity entity) {
        // Create map of existing items by serviceName
        Map<String, ServiceCombinationItemEntity> existingByServiceName = entity.getItems().stream()
                .collect(Collectors.toMap(ServiceCombinationItemEntity::getServiceName, i -> i));

        // Get service names from domain
        Set<String> domainServiceNames = domain.items().stream()
                .map(ServiceCombinationItem::serviceName)
                .collect(Collectors.toSet());

        // Remove items no longer in domain
        entity.getItems().removeIf(i -> !domainServiceNames.contains(i.getServiceName()));

        // Update existing or add new
        for (ServiceCombinationItem item : domain.items()) {
            ServiceCombinationItemEntity existing = existingByServiceName.get(item.serviceName());
            if (existing != null) {
                // Update existing
                existing.setItemOrder(item.order());
            } else {
                // Add new
                ServiceCombinationItemEntity newItem = new ServiceCombinationItemEntity();
                newItem.setCombination(entity);
                newItem.setServiceName(item.serviceName());
                newItem.setItemOrder(item.order());
                entity.getItems().add(newItem);
            }
        }
    }
}
