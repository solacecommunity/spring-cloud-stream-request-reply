package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import static ch.sbb.tms.platform.springbootstarter.requestreply.service.ReplyMessageChannel.CHANNEL_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
;
@Component(CHANNEL_NAME)
public class ReplyMessageChannel implements MessageChannel {
    public static final String CHANNEL_NAME = "requestReply-replies-in-0";

    @Autowired
    private RequestReplyService requestReplyService;

    @Override
    public boolean send(Message<?> message, long timeout) {
        requestReplyService.onReplyReceived(message);
        return true;
    }
}
