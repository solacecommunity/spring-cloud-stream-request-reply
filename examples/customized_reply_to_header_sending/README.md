# RequestReply: Customizable logging functionality

## Description

If the library's message format needs some tweaking, such as a replyTo header copy to another custom header, you can do
that defining your interceptor bean:

```java
public class CustomizedReplyToHeaderInterceptor implements RequestSendingInterceptor {
    @Override
    public <T> Message<T> interceptRequestSendingMessage(Message<T> message, String bindingName) {
        MessageBuilder<T> messageBuilder = MessageBuilder.fromMessage(message);
        // change the outline of your message here, if needed use the bindingName to distinguish different behavior per binder.
        return messageBuilder.build();
    }
}
---
@Configuration
public class CustomizedReplyToHeaderInterceptorConfig {
    @Bean
    public RequestSendingInterceptor requestSendingInterceptor() {
        return new CustomizedReplyToHeaderInterceptor(...);
    }
}
```

On the wrapping helper functions for the responder, you have te following bean:

```java
public class MyReplyWrappingInterceptor implements ReplyWrappingInterceptor {
    @Override
    public <T> Message<T> interceptReplyWrappingMessage(Message<T> message, String bindingName) {
        MessageBuilder<T> messageBuilder = MessageBuilder.fromMessage(message);
        // change the outline of your message here, if needed use the bindingName to distinguish different behavior per binder.
        return messageBuilder.build();
    }
}
---
@Configuration
public class CustomizedReplyToHeaderWrappingInterceptorConfig {
    @Bean
    public ReplyWrappingInterceptor replyWrappingInterceptor() {
        return new MyReplyWrappingInterceptor(bindingResolver);
    }
}
```

The purpose for this sample is to show the customized message interceptor in action.
You should start an instance of the example under "examples/customized_reply_to_header_response" in combination
with this application to have a full message circle in action.

## Setup

Usually you don't need to setup anything to run this example.
If for some reason you cannot access the public facing broker "public.messaging.solace.cloud",
its possible to set the environment variables to use your own broker:

SOLACE_MSG_VPN=yourVpn<br>
SOLACE_HOSTS=tcps://your-broker.messaging.solace.cloud:55443<br>
SOLACE_USERNAME=demo<br>
SOLACE_PASSWORD=demo<br>
HOSTNAME=[your computername]<br>

### STS

In STS/Eclipse you append the variables in the launch configuration. Once you have invoked
RequestReplySendingApplication.java you can configure the launch configuration. To easily add all variables at once
copy the entries above and use the "Paste" button in the button bar at the right of the variable list.

![Launch Config Dialog in STS](../images/sts1.png)
