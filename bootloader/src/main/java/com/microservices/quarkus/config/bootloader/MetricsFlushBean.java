package com.microservices.quarkus.config.bootloader;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forces OTLP meter registry to flush pending metrics on shutdown.
 * OtlpMeterRegistry (StepMeterRegistry) only publishes at step boundaries.
 * Calling close() triggers a final publish() before shutdown — critical
 * for short-lived test runs where the JVM exits between step intervals.
 */
@ApplicationScoped
public class MetricsFlushBean {

  private static final Logger log = LoggerFactory.getLogger(MetricsFlushBean.class);

  @Inject
  MeterRegistry meterRegistry;

  void onShutdown(@Observes ShutdownEvent event) {
    flushOtlpRegistries(meterRegistry);
  }

  private void flushOtlpRegistries(MeterRegistry registry) {
    if (registry instanceof OtlpMeterRegistry otlp) {
      log.info("Closing OTLP meter registry to flush pending metrics...");
      otlp.close();
      log.info("OTLP meter registry flush complete");
      return;
    }
    if (registry instanceof CompositeMeterRegistry composite) {
      for (MeterRegistry inner : composite.getRegistries()) {
        flushOtlpRegistries(inner);
      }
    }
  }
}
