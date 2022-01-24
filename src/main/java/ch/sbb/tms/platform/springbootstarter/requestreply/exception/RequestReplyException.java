/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.exception;

public class RequestReplyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RequestReplyException(Throwable cause) {
        super(cause);
    }
}
