package com.microservice.quarkus.catalog.tours.infrastructure.eventbus.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TourEventListener {

    private static final Logger LOG = Logger.getLogger(TourEventListener.class);

    @ConsumeEvent("catalog.tour.TourCreatedEvent")
    @Blocking
    public void onTourCreated(String event) {
        JsonObject jsonEvent = new JsonObject(event);
        LOG.infof("Tour created: code=%s, basePrice=%s, duration=%s",
                jsonEvent.getString("code"),
                jsonEvent.getString("basePrice"),
                jsonEvent.getString("duration"));
    }

    @ConsumeEvent("catalog.tour.TourPublishedEvent")
    @Blocking
    public void onTourPublished(String event) {
        JsonObject jsonEvent = new JsonObject(event);
        LOG.infof("Tour published: code=%s, basePrice=%s",
                jsonEvent.getString("code"),
                jsonEvent.getString("basePrice"));
    }

    @ConsumeEvent("catalog.tour.TourSuspendedEvent")
    @Blocking
    public void onTourSuspended(String event) {
        JsonObject jsonEvent = new JsonObject(event);
        LOG.infof("Tour suspended: code=%s, reason=%s",
                jsonEvent.getString("code"),
                jsonEvent.getString("reason"));
    }

    @ConsumeEvent("catalog.tour.TourDiscontinuedEvent")
    @Blocking
    public void onTourDiscontinued(String event) {
        JsonObject jsonEvent = new JsonObject(event);
        LOG.infof("Tour discontinued: code=%s",
                jsonEvent.getString("code"));
    }

    @ConsumeEvent("catalog.tour.ServiceAddedToTourEvent")
    @Blocking
    public void onServiceAddedToTour(String event) {
        JsonObject jsonEvent = new JsonObject(event);
        LOG.infof("Service added to tour: tourId=%s, serviceId=%s, serviceName=%s, isMandatory=%b",
                jsonEvent.getString("tourId"),
                jsonEvent.getString("serviceId"),
                jsonEvent.getString("serviceName"),
                jsonEvent.getBoolean("isMandatory"));
    }
}
