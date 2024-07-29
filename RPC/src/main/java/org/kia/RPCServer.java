package org.kia;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RPCServer {
    private static final String QUEUE_NAME = "rpc_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");

        var connection = factory.newConnection();
        var channel = connection.createChannel();
//        channel.queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String,Object> arguments)
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.queuePurge(QUEUE_NAME);
        channel.basicQos(1);
        System.out.println(" [x] Awaiting RPC requests");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            AMQP.BasicProperties properties = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();
            var response = "";
            try {
                var message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                int n = Integer.parseInt(message);
                response += fib(n);
            } catch (RuntimeException e) {
                System.out.println(e);
            } finally {
//                basicPublish(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body)
                channel.basicPublish("", delivery.getProperties().getReplyTo(), properties, response.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, (consumerTag -> {
        }));
    }

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }
}
