/**
 * package com.microservice.quarkus.payment.bootstrap;
 * 
 * import
 * com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateProductsOptionCommand;
 * import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
 * import
 * com.microservice.quarkus.payment.application.usecase.CreateProductsOptionUseCase;
 * 
 * import io.quarkus.runtime.StartupEvent;
 * import jakarta.annotation.Priority;
 * import jakarta.enterprise.context.ApplicationScoped;
 * import jakarta.enterprise.event.Observes;
 * import jakarta.enterprise.inject.Instance;
 * import jakarta.inject.Inject;
 * 
 * import java.util.List;
 * 
 * import org.jboss.logging.Logger;
 * 
 * /**
 * Bootstrap class for Payment module.
 * Initializes the payment context on application startup.
 * Priority 40 ensures it runs after IAM (10), Passenger (20), Admin (30).
 */
/**
 * @ApplicationScoped
 *                    public class PaymentBootstrap {
 * 
 *                    private static final Logger LOG =
 *                    Logger.getLogger(PaymentBootstrap.class);
 * 
 * @Inject
 *         Instance<VendureAdminGateway> vendureAdminGateway;
 * 
 * @Inject
 *         CreateProductsOptionUseCase createProductsOptionUseCase;
 * 
 *         void onStart(@Observes @Priority(40) StartupEvent event) {
 *         /*
 *         LOG.info(">> (40) Initializing Payment module...");
 * 
 *         // Check Vendure health
 *         checkVendureHealth();
 * 
 *         LOG.info(">> Payment module initialized");
 * 
 *         CreateProductsOptionCommand command = new
 *         CreateProductsOptionCommand("TICKET", List.of("normal", "vip"));
 *         createProductsOptionUseCase.execute(command);
 * 
 *         LOG.info("ProductOption Created");
 *         }
 * 
 */

/**
 * private void checkVendureHealth() {
 * if (!vendureAdminGateway.isResolvable()) {
 * LOG.warn(" VendureAdminGateway not available");
 * return;
 * }
 * 
 * VendureAdminGateway gateway = vendureAdminGateway.get();
 * 
 * LOG.info(" Checking Vendure Admin API health...");
 * 
 * if (gateway.isAvailable()) {
 * LOG.info(" [OK] Vendure Admin API is available");
 * } else {
 * LOG.warn(" Vendure Admin API is NOT available");
 * LOG.warn(" Vendure operations will fail until the API is reachable");
 * }
 * }
 * }
 **/
