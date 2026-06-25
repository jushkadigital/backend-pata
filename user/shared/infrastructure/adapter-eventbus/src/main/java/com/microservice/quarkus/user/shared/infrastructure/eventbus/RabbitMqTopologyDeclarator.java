package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RabbitMqTopologyDeclarator {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqTopologyDeclarator.class);
    private static final String[] DOMAINS = {"identity", "passenger", "notification"};

    @ConfigProperty(name = "rabbitmq-host")
    String host;

    @ConfigProperty(name = "rabbitmq-port")
    int port;

    @ConfigProperty(name = "rabbitmq-username")
    String username;

    @ConfigProperty(name = "rabbitmq-password")
    String password;

    @ConfigProperty(name = "rabbitmq-url")
    Optional<String> rabbitmqUrl;

    void onStart(@Observes @Priority(50) StartupEvent event) {
        try {
            ConnectionFactory factory = new ConnectionFactory();

            if (rabbitmqUrl.isPresent() && !rabbitmqUrl.get().isBlank()) {
                factory.setUri(rabbitmqUrl.get());
            } else {
                factory.setHost(host);
                factory.setPort(port);
                factory.setUsername(username);
                factory.setPassword(password);
            }

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                declareTopology(channel);
            }
            log.info("RabbitMQ topology declared successfully");
        } catch (Exception e) {
            log.error("Failed to declare RabbitMQ topology", e);
        }
    }

    private void declareTopology(Channel channel) throws Exception {
        for (String domain : DOMAINS) {
            String exchange = domain + ".events";
            String dlx = exchange + ".dlx";
            String mainQueue = exchange + ".queue";
            String retryQueue = exchange + ".retry.queue";
            String dlq = exchange + ".dlq";

            // Declare main exchange (topic, durable)
            channel.exchangeDeclare(exchange, "topic", true, false, null);

            // Declare DLX (fanout, durable)
            channel.exchangeDeclare(dlx, "fanout", true, false, null);

            // Declare main queue with DLX binding
            Map<String, Object> mainQueueArgs = Map.of("x-dead-letter-exchange", dlx);
            channel.queueDeclare(mainQueue, true, false, false, mainQueueArgs);

            // Bind main queue to exchange with routing key #
            channel.queueBind(mainQueue, exchange, "#");

            // Declare retry queue with TTL and DLX pointing back to main exchange
            Map<String, Object> retryQueueArgs = Map.of(
                    "x-dead-letter-exchange", exchange,
                    "x-message-ttl", 60000
            );
            channel.queueDeclare(retryQueue, true, false, false, retryQueueArgs);

            // Bind retry queue to DLX
            channel.queueBind(retryQueue, dlx, "");

            // Declare DLQ
            channel.queueDeclare(dlq, true, false, false, null);

            // Bind DLQ to DLX
            channel.queueBind(dlq, dlx, "");

            log.info("RabbitMQ topology declared for domain: {}", domain);
        }
    }
}
