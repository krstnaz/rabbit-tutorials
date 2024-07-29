package org.kia;

import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public class PublisherConfirms {
    private static final int MESSAGE_COUNT = 50_000;

    public static void main(String[] args) throws Exception {
        publishMessagesIndividually();
        publishMessagesInBatches();
        handlePublishConfirmsAsynchronously();
    }

    private static void publishMessagesIndividually() throws Exception {
        var connection = createConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            var queue = UUID.randomUUID().toString();
            //        channel.queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String,Object> arguments)
            channel.queueDeclare(queue, false, false, true, null);
            channel.confirmSelect();
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                channel.basicPublish("", queue, null, body.getBytes());
                channel.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    private static void publishMessagesInBatches() throws Exception {
        var connection = createConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            var queue = UUID.randomUUID().toString();
            //        channel.queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String,Object> arguments)
            channel.queueDeclare(queue, false, false, true, null);
            channel.confirmSelect();
            int batchSize = 100;
            int outstandingMessageCount = 0;
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                channel.basicPublish("", queue, null, body.getBytes());
                outstandingMessageCount++;

                if (outstandingMessageCount == batchSize) {
                    channel.waitForConfirmsOrDie(5_000);
                    outstandingMessageCount = 0;
                }
            }

            if (outstandingMessageCount > 0) {
                channel.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    private static void handlePublishConfirmsAsynchronously() throws Exception {
        var connection = createConnection();
        var channel = connection.createChannel();
        try (connection; channel) {
            String queue = UUID.randomUUID().toString();
            channel.queueDeclare(queue, false, false, true, null);
            channel.confirmSelect();
            ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
            ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
                if (multiple) {
                    ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(
                            sequenceNumber, true
                    );
                    confirmed.clear();
                } else {
                    outstandingConfirms.remove(sequenceNumber);
                }
            };
            channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
                String body = outstandingConfirms.get(sequenceNumber);
                System.err.format(
                        "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                        body, sequenceNumber, multiple
                );
                cleanOutstandingConfirms.handle(sequenceNumber, multiple);
            });
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
                channel.basicPublish("", queue, null, body.getBytes());
            }

            if (!waitUntil(Duration.ofSeconds(60), () -> outstandingConfirms.isEmpty())) {
                throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    private static Connection createConnection() throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPassword("rmpassword");
        factory.setUsername("rmuser");
        return factory.newConnection();
    }

    private static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited += 100;
        }
        return condition.getAsBoolean();
    }
}
