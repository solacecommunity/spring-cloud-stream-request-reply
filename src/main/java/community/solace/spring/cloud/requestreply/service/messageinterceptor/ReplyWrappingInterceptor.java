package community.solace.spring.cloud.requestreply.service.messageinterceptor;

import org.springframework.messaging.Message;

/**
 * For adjustments on messages before they are replied on the wrapping helper of request-reply
 * Use this interface by creating a bean named replyWrappingInterceptor,
 * that implements this interface.
 *
 * Note: Typically you should implement all three messages since error handling and i.e. empty messages
 * should be treated well by your extended functionality, for this reason it has no default implementation
 * built into this interface.
 */
public interface ReplyWrappingInterceptor {
    /**
     * Normal operation when a message is replied.
     * @param message the message, edit this and return the modified message object
     * @param bindingName to distinguish binder specific behavior
     * @return the modified message
     * @param <T> Type of message payload
     */
    <T> Message<T> interceptReplyWrappingPayloadMessage(Message<T> message, String bindingName);
    /**
     * Last message that declares the end of a message stream. If no objects are found by a
     * replier, this is the only message that is sent.
     * @param message the message, edit this and return the modified message object
     * @param bindingName to distinguish binder specific behavior
     * @return the modified message
     * @param <T> Type of message payload
     */
    <T> Message<T> interceptReplyWrappingFinishingEmptyMessage(Message<T> message, String bindingName);
    /**
     * On any error case that the responder wants to reply to the requestor, this method is used with the
     * message containing info about the error that happened.
     * @param message the message, edit this and return the modified message object
     * @param bindingName to distinguish binder specific behavior
     * @return the modified message
     * @param <T> Type of message payload
     */
    <T> Message<T> interceptReplyWrappingErrorMessage(Message<T> message, String bindingName);
}
