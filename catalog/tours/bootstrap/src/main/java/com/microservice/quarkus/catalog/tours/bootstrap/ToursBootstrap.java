/**
 * package com.microservice.quarkus.catalog.tours.bootstrap;
 * 
 * import io.quarkus.runtime.StartupEvent;
 * import jakarta.annotation.Priority;
 * import jakarta.enterprise.context.ApplicationScoped;
 * import jakarta.enterprise.event.Observes;
 * import org.jboss.logging.Logger;
 * 
 * @ApplicationScoped
 *                    public class ToursBootstrap {
 * 
 *                    private static final Logger LOG =
 *                    Logger.getLogger(ToursBootstrap.class);
 * 
 *                    void onStart(@Observes @Priority(50) StartupEvent ev) {
 *                    LOG.info(">> (50) Iniciando Catalog Tours Bootstrap...");
 * 
 *                    try {
 *                    initializeTours();
 *                    } catch (Exception e) {
 *                    LOG.errorf("Catalog Tours Bootstrap: Error during
 *                    initialization: %s", e.getMessage());
 *                    e.printStackTrace();
 *                    }
 *                    }
 * 
 *                    private void initializeTours() {
 *                    // Tours can be created via REST API
 *                    // No default tours are created at startup
 *                    LOG.info("Catalog Tours Bootstrap completed. Tours can be
 *                    created via REST API at /api/catalog/tours");
 *                    }
 *                    }
 **/
