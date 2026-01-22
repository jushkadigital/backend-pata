package com.microservice.quarkus.catalog.tours.infrastructure.rest.mapper;

import com.microservice.quarkus.catalog.tours.domain.IncludedService;
import com.microservice.quarkus.catalog.tours.domain.ServiceCombination;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.domain.TourPolicy;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.IncludedServiceDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.ServiceCombinationDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.TourDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.TourPolicyDTO;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface TourRestMapper {

    default TourDTO toDTO(Tour tour) {
        if (tour == null) {
            return null;
        }

        TourDTO dto = new TourDTO();
        dto.setId(tour.getId().value());
        dto.setCode(tour.getCode().value());
        dto.setName(tour.getName());
        dto.setDescription(tour.getDescription());
        dto.setDurationHours(tour.getDuration().hours());
        dto.setStatus(TourDTO.StatusEnum.fromValue(tour.getStatus().name()));
        dto.setVersion(tour.getVersion());
        dto.setParentTourId(tour.getParentTourId() != null ? tour.getParentTourId().value() : null);

        List<IncludedServiceDTO> serviceDTOs = tour.getIncludedServices().stream()
                .map(this::toServiceDTO)
                .collect(Collectors.toList());
        dto.setIncludedServices(serviceDTOs);

        List<TourPolicyDTO> policyDTOs = tour.getPolicies().stream()
                .map(this::toPolicyDTO)
                .collect(Collectors.toList());
        dto.setPolicies(policyDTOs);

        List<ServiceCombinationDTO> combinationDTOs = tour.getServiceCombinations().stream()
                .map(this::toCombinationDTO)
                .collect(Collectors.toList());
        dto.setServiceCombinations(combinationDTOs);

        dto.setCreatedAt(toDate(tour.getCreatedAt()));
        dto.setUpdatedAt(toDate(tour.getUpdatedAt()));

        return dto;
    }

    default IncludedServiceDTO toServiceDTO(IncludedService service) {
        IncludedServiceDTO dto = new IncludedServiceDTO();
        dto.setServiceType(IncludedServiceDTO.ServiceTypeEnum.fromValue(service.serviceType().name()));
        dto.setServiceName(service.serviceName());
        dto.setQuantity(service.quantity());
        dto.setIsMandatory(service.isMandatory());
        dto.setDurationHours(service.durationHours());
        return dto;
    }

    default TourPolicyDTO toPolicyDTO(TourPolicy policy) {
        TourPolicyDTO dto = new TourPolicyDTO();
        dto.setType(TourPolicyDTO.TypeEnum.fromValue(policy.type().name()));
        dto.setDescription(policy.description());
        dto.setValue(policy.value());
        return dto;
    }

    default ServiceCombinationDTO toCombinationDTO(ServiceCombination combination) {
        ServiceCombinationDTO dto = new ServiceCombinationDTO();
        dto.setName(combination.name());
        dto.setDescription(combination.description());
        dto.setSku(combination.sku());
        dto.setServices(combination.getServiceNames());
        return dto;
    }

    default List<TourDTO> toDTOList(List<Tour> tours) {
        return tours.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    default Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
