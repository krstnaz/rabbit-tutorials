package org.kia;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Producer {
    private static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            var msg = "Hello world";
            channel.basicPublish("", QUEUE_NAME, null, msg.getBytes());
            System.out.println("Produce message: " + msg);
        }
    }
}
