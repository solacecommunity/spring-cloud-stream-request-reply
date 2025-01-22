package community.solace.spring.cloud.requestreply.service.messageinterceptor;

import org.springframework.messaging.Message;

/**
 * For adjustments on messages before they are sent out with request-reply
 * Use this interface by creating a bean named requestSendingInterceptor,
 * that implements this interface.
 */
public interface RequestSendingInterceptor {
    /**
     * Before a message is sent through you can add headers or change payload of the message.
     * The binding name is an argument if you need to distinguish your behavior based on different
     * binders i.e. multiple request reply destinations in one project.
     *
     * @param message the message, edit this and return the modified message object
     * @param bindingName to distinguish binder specific behavior
     * @return the modified message
     * @param <T> Type of message payload
     */
    <T> Message<T> interceptRequestSendingMessage(Message<T> message, String bindingName);
}
