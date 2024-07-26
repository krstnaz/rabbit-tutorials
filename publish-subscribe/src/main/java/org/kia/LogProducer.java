package org.kia;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class LogProducer {
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            var message = "some log";
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent log: " + message);
        }
    }
}
