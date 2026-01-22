package com.microservice.quarkus.catalog.tours.application.service;

import com.microservice.quarkus.catalog.tours.application.api.TourApiService;
import com.microservice.quarkus.catalog.tours.application.dto.AddPolicyToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.CreateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.PublishTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.SuspendTourCommand;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TourRepositoryImpl implements TourApiService {

    @Inject
    TourService tourService;

    @Override
    public String create(CreateTourCommand cmd) {
        return tourService.create(cmd);
    }

    @Override
    public void addService(AddServiceToTourCommand cmd) {
        tourService.addService(cmd);
    }

    @Override
    public void addPolicy(AddPolicyToTourCommand cmd) {
        tourService.addPolicy(cmd);
    }

    @Override
    public void publish(PublishTourCommand cmd) {
        tourService.publish(cmd);
    }

    @Override
    public void suspend(SuspendTourCommand cmd) {
        tourService.suspend(cmd);
    }

    @Override
    public void resume(String tourId) {
        tourService.resume(tourId);
    }

    @Override
    public void discontinue(String tourId) {
        tourService.discontinue(tourId);
    }

    @Override
    public Optional<Tour> findById(String tourId) {
        return tourService.findById(tourId);
    }

    @Override
    public Optional<Tour> findByCode(String code) {
        return tourService.findByCode(code);
    }

    @Override
    public List<Tour> findAll() {
        return tourService.findAll();
    }

    @Override
    public List<Tour> findByStatus(String status) {
        return tourService.findByStatus(status);
    }

    @Override
    public List<Tour> findPublished() {
        return tourService.findPublished();
    }

    @Override
    public List<Tour> findSellable() {
        return tourService.findSellable();
    }

    @Override
    public void delete(String tourId) {
        tourService.delete(tourId);
    }
}
