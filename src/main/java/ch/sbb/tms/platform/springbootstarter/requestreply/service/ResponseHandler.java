package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.util.concurrent.Phaser;
import java.util.function.Consumer;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage.RemoteErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

public class ResponseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);

    private final Phaser countDownLatch;
    private final boolean supportMultipleResponses;

    private final Consumer<Message<?>> responseMessageConsumer;

    private boolean isFirstMessage = true;
    private String errorMessage;

    public ResponseHandler(Consumer<Message<?>> responseMessageConsumer, boolean supportMultipleResponses) {
        this.countDownLatch = new Phaser(2); // 1 for self|await and 1 for first message
        this.responseMessageConsumer = responseMessageConsumer;
        this.supportMultipleResponses = supportMultipleResponses;
    }

    public void receive(Message<?> message) {
        LOG.debug("received response {}", message);

        isFirstMessage = false;

        responseMessageConsumer.accept(message);
        countDownLatch.arriveAndDeregister();
    }

    public void await() throws RemoteErrorException {
        countDownLatch.arriveAndAwaitAdvance();
        if (StringUtils.hasText(errorMessage)) {
            throw new RemoteErrorException(errorMessage);
        }
    }

    public void setTotalReplies(Integer totalReplies) {
        if (supportMultipleResponses && isFirstMessage && totalReplies > 1) {
            // Set total messages to expect when multi message on first message.
            countDownLatch.bulkRegister(totalReplies - 1); // the first message is already registered.
        }
    }

    public void emptyResponse() {
        isFirstMessage = false;

        countDownLatch.arriveAndDeregister();
    }

    public void errorResponse(String errorMessage) {
        isFirstMessage = false;
        this.errorMessage = errorMessage;
        countDownLatch.arriveAndDeregister();
    }
}
