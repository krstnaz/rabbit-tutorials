package org.kia;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        var prefetchCount = 1;
        channel.basicQos(prefetchCount);
        var durable = true;
        channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            var msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Consume message: " + msg);
            try {
                doWork(msg);
            } catch (InterruptedException e) {
                System.out.println(e);
            } finally {
                System.out.println(OffsetDateTime.now() + " Consuming work is done");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        var autoAck = false;
        channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, consumerTag -> {
        });
    }

    private static void doWork(String task) throws InterruptedException {
        for (char ch : task.toCharArray()) {
            if (ch == '.') Thread.sleep(1000);
        }
    }
}
