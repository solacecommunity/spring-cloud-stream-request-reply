/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.exception;

public class RequestReplyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RequestReplyException() {
        super();
    }

    public RequestReplyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RequestReplyException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestReplyException(String message) {
        super(message);
    }

    public RequestReplyException(Throwable cause) {
        super(cause);
    }
}
