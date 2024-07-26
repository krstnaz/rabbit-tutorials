package org.kia;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class LogProducer {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            var debugMessage = "debugMessage";
            channel.basicPublish(EXCHANGE_NAME, "debug", null, debugMessage.getBytes(StandardCharsets.UTF_8));
            var infoMessage = "infoMessage";
            channel.basicPublish(EXCHANGE_NAME, "info", null, infoMessage.getBytes(StandardCharsets.UTF_8));
            var errorMessage = "errorMessage";
            channel.basicPublish(EXCHANGE_NAME, "error", null, errorMessage.getBytes(StandardCharsets.UTF_8));
        }
    }
}
