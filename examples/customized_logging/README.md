# RequestReply: Customizable logging functionality

## Description

If the library's logging content is not the way library users want it, a user can define a spring bean which lets
them define three logging methods:

```
public interface RequestReplyLogger {
    void logRequest(Logger logger, Level suggestedLevel, String suggestedLogMessage, Message<?> message);
    void logReply(Logger logger, Level suggestedLevel, String suggestedLogMessage, long remainingReplies, Message<?> message);
    void log(Logger logger, Level suggestedLevel, String suggestedLogMessage, Object... formatArgs);
}
```

The purpose for this sample is to show the customized logging in action.
Note the following 2 requirements (already present for you if you didn't change anything):

- The log4j2.xml file is required to configure the basic log appender, otherwise spring will chicken-out and do not the
  appropriate logging.
- A requestReplyLogger bean present within a "@Configuration"-annotated config-element (Done for you in
  community.solace.spring.cloud.requestreply.examples.sending.config.CustomizedReplyToHeaderInterceptorConfig.

Once you start the application, requests are scheduled, and you should start one instance of the
example/request_reply_response application.
Then you will get answers with custom format logging like this:

```
2025-01-14T11:52:34.389+0100 DEBUG >>> SensorReading [ 2025-01-14 11:52:34.159 demo 10.2 CELSIUS ] {solace_scst_messageVersion=1, solace_expiration=0, solace_destination=requestReply/response/solace/d-pampelmuse/dynamic/example_custom_logging_K55577_678172fd-40de-4617-b1fc-edcadcbd0e6a, solace_replicationGroupMessageId=rmid1:3ab62-1d466d0ca43-00000000-0000de17, deliveryAttempt=1, solace_isReply=false, solace_timeToLive=0, solace_receiveTimestamp=0, acknowledgmentCallback=com.solace.spring.cloud.stream.binder.inbound.acknowledge.JCSMPAcknowledgementCallback@35b0903b, solace_discardIndication=false, solace_dmqEligible=true, solace_priority=4, correlationId=24cbd37e-2746-45b9-8859-fc160681ccd2, solace_redelivered=false, id=a0847ea1-f180-0e8c-c2c7-3bf0a83f709b, contentType=application/json, timestamp=1736851954353} remaining replies: 0
2025-01-14T11:53:34.171+0100 DEBUG generated correlation Id 28315c19-5694-478e-98db-8cf86212a161 for request directed to last_value/temperature/celsius/forLoggingDemo with content SensorReading [ 2025-01-14 11:53:34.171 demo n null ]
2025-01-14T11:53:34.172+0100 DEBUG Using binding:requestReplyRepliesDemoSolaceDynamic , destination:last_value/temperature/celsius/forLoggingDemo and replyTopic:requestReply/response/solace/{StagePlaceholder}/dynamic/example_custom_logging_K55577_678172fd-40de-4617-b1fc-edcadcbd0e6a
2025-01-14T11:53:34.173+0100 TRACE Querying correlationId 28315c19-5694-478e-98db-8cf86212a161
2025-01-14T11:53:34.173+0100 DEBUG <<< SensorReading [ 2025-01-14 11:53:34.171 demo n null ] {correlationId=28315c19-5694-478e-98db-8cf86212a161, id=c4af73b4-b8d7-f183-c064-80375bfa564b, groupedMessages=true, customTestHeader=requestReply/response/solace/{StagePlaceholder}/dynamic/example_custom_logging_K55577_678172fd-40de-4617-b1fc-edcadcbd0e6a, scst_targetDestination=last_value/temperature/celsius/forLoggingDemo, timestamp=1736852014173}
2025-01-14T11:53:34.237+0100 TRACE Disregarding correlationId 28315c19-5694-478e-98db-8cf86212a161
```

If you choose not to start the example/request_reply_response application, you can still see the request part of your
scheduling
application, however you see as well the timeout exception for no response:

```
2025-01-14T11:51:34.181+0100 DEBUG <<< SensorReading [ 2025-01-14 11:51:34.165 demo n null ] {correlationId=c251abeb-23ef-46b5-b294-6b48f1c5ebb6, id=6cce1562-9eaa-226c-0cda-75623105b72f, groupedMessages=true, customTestHeader=requestReply/response/solace/{StagePlaceholder}/dynamic/example_custom_logging_K55577_678172fd-40de-4617-b1fc-edcadcbd0e6a, scst_targetDestination=last_value/temperature/celsius/forLoggingDemo, timestamp=1736851894176}
2025-01-14T11:51:34.255+0100 INFO Creating producer to TOPIC requestReplyRepliesDemoSolaceDynamic-out-0 <message handler ID: 0806211e-fa62-4643-90a9-ce40ecd3f2f3>
2025-01-14T11:51:34.255+0100 INFO No producer exists, a new one will be created
2025-01-14T11:51:34.304+0100 INFO Channel 'unknown.channel.name' has 1 subscriber(s).
2025-01-14T11:52:04.181+0100 TRACE Disregarding correlationId c251abeb-23ef-46b5-b294-6b48f1c5ebb6
2025-01-14T11:52:04.182+0100 ERROR Failed to collect response for correlationId c251abeb-23ef-46b5-b294-6b48f1c5ebb6: class java.util.concurrent.CompletionException: java.lang.RuntimeException: java.util.concurrent.TimeoutException
2025-01-14T11:52:04.185+0100 ERROR Unexpected error occurred in scheduled task
java.util.concurrent.TimeoutException: Failed to collect response: class java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.util.concurrent.TimeoutException
	at community.solace.spring.cloud.requestreply.service.RequestReplyServiceImpl.wrapTimeOutException(RequestReplyServiceImpl.java:429) ~[spring-cloud-stream-starter-request-reply-5.2.1-SNAPSHOT.jar:5.2.1-SNAPSHOT]
	at community.solace.spring.cloud.requestreply.service.RequestReplyServiceImpl.requestAndAwaitReplyToTopic(RequestReplyServiceImpl.java:127) ~[spring-cloud-stream-starter-request-reply-5.2.1-SNAPSHOT.jar:5.2.1-SNAPSHOT]
	at community.solace.spring.cloud.requestreply.service.RequestReplyService.requestAndAwaitReplyToTopic(RequestReplyService.java:33) ~[spring-cloud-stream-starter-request-reply-5.2.1-SNAPSHOT.jar:5.2.1-SNAPSHOT]
	at community.solace.spring.cloud.requestreply.examples.sending.service.ScheduledRequestService.sendRequest(ScheduledRequestService.java:29) ~[classes/:?]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[?:?]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[?:?]
	at org.springframework.scheduling.support.ScheduledMethodRunnable.runInternal(ScheduledMethodRunnable.java:130) ~[spring-context-6.1.8.jar:6.1.8]
	at org.springframework.scheduling.support.ScheduledMethodRunnable.lambda$run$2(ScheduledMethodRunnable.java:124) ~[spring-context-6.1.8.jar:6.1.8]
	at io.micrometer.observation.Observation.observe(Observation.java:499) ~[micrometer-observation-1.13.0.jar:1.13.0]
	at org.springframework.scheduling.support.ScheduledMethodRunnable.run(ScheduledMethodRunnable.java:124) ~[spring-context-6.1.8.jar:6.1.8]
	at org.springframework.scheduling.support.DelegatingErrorHandlingRunnable.run(DelegatingErrorHandlingRunnable.java:54) ~[spring-context-6.1.8.jar:6.1.8]
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572) ~[?:?]
	at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java:358) ~[?:?]
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305) ~[?:?]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144) ~[?:?]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642) ~[?:?]
	at java.base/java.lang.Thread.run(Thread.java:1583) [?:?]
```

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
