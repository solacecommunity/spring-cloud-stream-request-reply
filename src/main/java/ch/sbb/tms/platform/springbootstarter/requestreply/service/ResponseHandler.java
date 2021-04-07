/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

class ResponseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);

    private final CountDownLatch countDownLatch;

    private Consumer<Message<?>> responseMessageConsumer;

    public ResponseHandler(Consumer<Message<?>> responseMessageConsumer) {
        this.countDownLatch = new CountDownLatch(1);
        this.responseMessageConsumer = responseMessageConsumer;
    }

    public void receive(Message<?> message) {
        LOG.debug("received response {}", message);
        responseMessageConsumer.accept(message);
        countDownLatch.countDown();
    }

    public void await() throws InterruptedException {
        countDownLatch.await();
    }
}
