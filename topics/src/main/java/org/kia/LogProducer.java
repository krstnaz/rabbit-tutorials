package org.kia;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class LogProducer {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            var debugMessage = "testDebugMessage";
            channel.basicPublish(EXCHANGE_NAME, "test.debug", null, debugMessage.getBytes(StandardCharsets.UTF_8));
            var infoMessage = "prodInfoMessage";
            channel.basicPublish(EXCHANGE_NAME, "prod.info", null, infoMessage.getBytes(StandardCharsets.UTF_8));
            var errorMessage = "localErrorMessage";
            channel.basicPublish(EXCHANGE_NAME, "local.error", null, errorMessage.getBytes(StandardCharsets.UTF_8));
            var prodErrorMessage = "prodErrorMessage";
            channel.basicPublish(EXCHANGE_NAME, "prod.error", null, prodErrorMessage.getBytes(StandardCharsets.UTF_8));
        }
    }
}
