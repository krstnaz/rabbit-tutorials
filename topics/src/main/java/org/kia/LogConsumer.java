package org.kia;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LogConsumer {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        var connection = factory.newConnection();
        var channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        var commonQueueName = channel.queueDeclare().getQueue();
        var errorQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(commonQueueName, EXCHANGE_NAME, "*.*");
        channel.queueBind(errorQueueName, EXCHANGE_NAME, "*.error");
        DeliverCallback commonDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(delivery.getEnvelope().getRoutingKey());
            System.out.println(" [x] Received in common'" + message + "'");
        };
        DeliverCallback errorDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(delivery.getEnvelope().getRoutingKey());
            System.out.println(" [x] Received in error'" + message + "'");
        };
        channel.basicConsume(commonQueueName, true, commonDeliverCallback, consumerTag -> {
        });
        channel.basicConsume(errorQueueName, true, errorDeliverCallback, consumerTag -> {
        });
    }
}
