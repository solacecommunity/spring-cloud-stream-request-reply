/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package community.solace.spring.cloud.requestreply.exception;

public class RequestReplyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RequestReplyException(Throwable cause) {
        super(cause);
    }
}
