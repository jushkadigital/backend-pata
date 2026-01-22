package com.microservice.quarkus.catalog.tours.infrastructure.rest.api;

import com.microservice.quarkus.catalog.tours.application.dto.AddPolicyToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceCombinationCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.CreateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.RemoveServiceCommand;
import com.microservice.quarkus.catalog.tours.application.dto.UpdateServiceCommand;
import com.microservice.quarkus.catalog.tours.application.dto.UpdateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.PublishTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.ServiceCommand;
import com.microservice.quarkus.catalog.tours.application.dto.SuspendTourCommand;
import com.microservice.quarkus.catalog.tours.application.service.TourService;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.AddPolicyRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.AddServiceCombinationRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.AddServiceRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.CreateTourRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.SuspendTourRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.TourDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.UpdateServiceRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.dto.UpdateTourRequestDTO;
import com.microservice.quarkus.catalog.tours.infrastructure.rest.mapper.TourRestMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TourResource implements ToursAPI {

    @Inject
    TourService tourService;

    @Inject
    TourRestMapper mapper;

    @Override
    public Response getAllTours(String status) {
        List<Tour> tours;

        if (status != null && !status.isBlank()) {
            tours = tourService.findByStatus(status);
        } else {
            tours = tourService.findAll();
        }

        return Response.ok(mapper.toDTOList(tours)).build();
    }

    @Override
    public Response getPublishedTours() {
        List<Tour> tours = tourService.findPublished();
        return Response.ok(mapper.toDTOList(tours)).build();
    }

    @Override
    public Response getSellableTours() {
        List<Tour> tours = tourService.findSellable();
        return Response.ok(mapper.toDTOList(tours)).build();
    }

    @Override
    public Response createTour(CreateTourRequestDTO request) {
        try {
            List<ServiceCommand> services = new ArrayList<>();
            if (request.getServices() != null) {
                for (AddServiceRequestDTO svc : request.getServices()) {
                    services.add(new ServiceCommand(
                            svc.getServiceType().name(),
                            svc.getServiceName(),
                            svc.getQuantity(),
                            svc.getIsMandatory() != null ? svc.getIsMandatory() : false,
                            svc.getDurationHours(),
                            null
                    ));
                }
            }

            CreateTourCommand cmd = new CreateTourCommand(
                    request.getCode(),
                    request.getName(),
                    request.getDescription(),
                    request.getDurationHours(),
                    services
            );
            String tourId = tourService.create(cmd);
            TourDTO response = tourService.findById(tourId)
                    .map(mapper::toDTO)
                    .orElseThrow();
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getTourById(String tourId) {
        return tourService.findById(tourId)
                .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public Response updateTour(String tourId, UpdateTourRequestDTO request) {
        try {
            UpdateTourCommand cmd = new UpdateTourCommand(
                    tourId,
                    request.getName(),
                    request.getDescription(),
                    request.getDurationHours()
            );
            tourService.updateTour(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getTourByCode(String code) {
        return tourService.findByCode(code)
                .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public Response deleteTour(String tourId) {
        try {
            tourService.delete(tourId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Override
    public Response addServiceToTour(String tourId, AddServiceRequestDTO request) {
        try {
            AddServiceToTourCommand cmd = new AddServiceToTourCommand(
                    tourId,
                    request.getServiceType().name(),
                    request.getServiceName(),
                    request.getQuantity(),
                    request.getIsMandatory() != null ? request.getIsMandatory() : false,
                    request.getDurationHours(),
                    null
            );
            tourService.addService(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response updateServiceInTour(String tourId, String serviceName, UpdateServiceRequestDTO request) {
        try {
            UpdateServiceCommand cmd = new UpdateServiceCommand(
                    tourId,
                    serviceName,
                    request.getServiceName(),
                    request.getServiceType() != null ? request.getServiceType().name() : null,
                    request.getQuantity(),
                    request.getIsMandatory(),
                    request.getDurationHours()
            );
            tourService.updateService(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response removeServiceFromTour(String tourId, String serviceName) {
        try {
            RemoveServiceCommand cmd = new RemoveServiceCommand(tourId, serviceName);
            tourService.removeService(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response addPolicyToTour(String tourId, AddPolicyRequestDTO request) {
        try {
            AddPolicyToTourCommand cmd = new AddPolicyToTourCommand(
                    tourId,
                    request.getPolicyType().name(),
                    request.getDescription(),
                    request.getValue()
            );
            tourService.addPolicy(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response addServiceCombinationToTour(String tourId, AddServiceCombinationRequestDTO request) {
        try {
            AddServiceCombinationCommand cmd = new AddServiceCombinationCommand(
                    tourId,
                    request.getName(),
                    request.getDescription(),
                    request.getSku(),
                    request.getServices(),
                    request.getDraftPriceInCents()
            );
            tourService.addServiceCombination(cmd);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response publishTour(String tourId) {
        try {
            tourService.publish(new PublishTourCommand(tourId));
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response suspendTour(String tourId, SuspendTourRequestDTO request) {
        try {
            tourService.suspend(new SuspendTourCommand(tourId, request.getReason()));
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response resumeTour(String tourId) {
        try {
            tourService.resume(tourId);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response discontinueTour(String tourId) {
        try {
            tourService.discontinue(tourId);
            return tourService.findById(tourId)
                    .map(tour -> Response.ok(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response createNewVersion(String tourId) {
        try {
            String newVersionId = tourService.createNewVersion(tourId);
            return tourService.findById(newVersionId)
                    .map(tour -> Response.status(Response.Status.CREATED).entity(mapper.toDTO(tour)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getTourVersionsByCode(String code) {
        List<Tour> versions = tourService.findAllVersionsByCode(code);
        return Response.ok(mapper.toDTOList(versions)).build();
    }
}
