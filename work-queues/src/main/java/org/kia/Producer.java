package org.kia;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

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
        var durable = true;
        try (connection; channel) {
            channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
            for (int i = 0; i < 6; i++) {
                var msg = "Hello" + ".".repeat(i);
                channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, msg.getBytes());
                System.out.println("Produce message: " + msg);
            }
        }
    }
}
