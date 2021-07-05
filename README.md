# `spring-boot-starter-request-reply`
## Description
This Spring Boot Starter provides request-reply functionality for [Spring Cloud Stream Binders].



The code is based on the original code created in the [ESTA Spring Cloud Stream Sample](https://code.sbb.ch/projects/TP_TMS_PLATTFORM/repos/springcloudstream-examples/browse).



## Usage
### Dependencies
In order to be able to use the request/reply functionality, add the following section to your Maven pom:

```xml
<dependency>
    <groupId>ch.sbb.tms.platform</groupId>
    <artifactId>spring-boot-starter-request-reply</artifactId>
</dependency>
```



In order to use the starter in conjunction with a Binder, one will also need the respective dependency for that one.



### Configuration

#### Default Binder

When dealing with multiple binders, but using request reply functionality only with one of them, one might set `spring.cloud.stream.defaultBinder` to that binder's name in order to be able to utilize the shortcut functions as described in the API section. E.g.

```yaml
spring:
  cloud:
    stream:
      defaultBinder: main_session
```



#### Sender / Receiver
The Request/Reply Spring Boot Starter can be used both in requesting and replying services. It can operate both via [MessageChannels] as well as [Spring Cloud Stream Binders].



##### Binder-less operation

If not having binders configured, the `RequestReplyService` API (see below) can be used to direct requests and receive responses at Spring [MessageChannels]. Responses can also be fed back into the system by sending them through channel `requestReply-replies-in-0` as defined in `ch.sbb.tms.platform.springbootstarter.requestreply.service.ReplyMessageChannel.CHANNEL_NAME`. This can be helpful when writing integration tests.



##### Binder operation

The Request/Reply Spring Boot Starter can operate on multiple binders at a time by adopting the central `RequestReplyService` to particular binders. Such an adapter will be created for each binder where there is the environment variable `requestReply.topic` defined.



The following `requestReply` properties are used to configure a reply consumer for a binder:

- `topic` _Required, Default: null_
  This topic will be used to listen for and receive replies at the particular binder. If the topic is not provided, no binder will be created.
- `group` _Optional, Default: null_
  This group will be sent to the binder during the binding process and is otherwise not of interest for the service. Note that an exception will be thrown in case the `consumerProperties` are configured as `partitioned` but the group is null.
- `timeout` _Optional, Default: 500 MILLISECONDS_
  Duration and [TimeUnit](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/TimeUnit.html) for the RequestReplyService to await replies before timing out
- `consumerProperties` _Optional, Default: empty_
  a map of properties that can be used to define the full range of binder specific [ConsumerProperties]. E.g. in case of a Solace Binder that would be [ExtendedConsumerProperties]&lt;[SolaceConsumerProperties]&gt;



Such a configuration would look like the example below:

```yaml
spring:
  cloud:
    stream:
	  binders:
        main_session:
          type: solace
          environment:
            requestReply:
              topic: requestreply/testapp/replies
              group: ${spring.cloud.stream.default.group}
              timeout:
                duration: 1
                unit: MINUTES
              consumerProperties:
                concurrency: 1
                extension:
                  provisionDurableQueue: true
                  queueRespectsMsgTtl: true
```



##### Hybrid operation

In order to avoid having hardcoded topics in the application one may follow the configurative approach by defining a `spring.cloud.stream.source` for the request sending part and configuring its target destination accordingly. E.g.:

```yaml
spring:
  cloud:
    stream:
      source: requestReply-mainSession-requests

      bindings:
        requestReply-mainSession-requests-out-0:
          binder: main_session
          destination: requestreply/testapp/requests
          contentType: "text/plain"
```



### API

#### `RequestReplyService`

The request/reply functionality provided by this starter can be utilized by autowiring the `RequestReplyService` which offers  the following methods:

- `requestAndAwaitReply(Q request, String requestDestination, Class<A> expectedResponseClass, Binder<?, ?, ?> binder)`
  sends the given request to the given request destination using the given binder, awaits the response and maps it to the provided class as a return value
- `requestAndAwaitReply(Q request, String requestDestination, Class<A> expectedResponseClass, String binderName)`
  sends the given request to the given request destination using the binder identified by the given name, awaits the response and maps it to the provided class as a return value
- `requestReply(Q request, String requestDestination, String replyDestination, Binder<?, ?, ?> binder)`
  sends the given request to the given request destination using the given binder, telling it to direct replies at the given reply destination
- `requestReply(Q request, String requestDestination, String replyDestination, String binderName)`
  sends the given request to the given request destination using the binder identified by the given name, telling it to direct replies at the given reply destination

In case a `spring.cloud.stream.defaultBinder` is defined and/or only one binder is defined at all, one may also use the following equivalents as a shortcut for the default binder:

- `requestReply(Q request, String requestDestination, String replyDestination)`
- `requestAndAwaitReply(Q request, String requestDestination, Class<A> expectedClass)`



__Note:__
- request and reply destinations can be both [MessageChannels] within the same system as well as queues respectively topics handled by the respective binder
- the `RequestReplyService` uses the Spring Integration Argument Resolver Message Converter to convert between the data types of different message channels (see [Payload Type Conversion @ Spring Integration Documentation](https://docs.spring.io/spring-integration/docs/5.1.4.RELEASE/reference/html/#payload-type-conversion)). In order to create custom converters, have a look at [Guide to Spring Type Conversions @ Baeldung](https://www.baeldung.com/spring-type-conversions) .
- request channels that receive requests must pay respect to the correlation id set in either `IntegrationMessageHeaderAccessor.CORRELATION_ID` or `SolaceHeaders.CORRELATION_ID` to create respective responses<br/>
  if you implement such functions on your own, you might use `RequestReplyMessageHeaderSupportService` to wrap them (see below)
- request channels that receive requests must pay respect to the reply channel set in either `MessageHeaders.REPLY_CHANNEL` or `SolaceHeaders.REPLY_TO` to direct respective responses to<br/>
  if you implement such functions on your own, you might use `RequestReplyMessageHeaderSupportService` to wrap them (see below)



#### `RequestReplyMessageHeaderSupportService`

In case a service is only responding to requests, the library can still be used as it offers receiving services helper methods to wrap response functions by means of the `RequestReplyMessageHeaderSupportService`, which properly sets the Message's destination header, which then can be picked up through Spring Cloud Functions. e.g.:

```java
    @Bean
    @Autowired
    public Function<Message<String>, Message<String>> reverse(RequestReplyMessageHeaderSupportService headerSupport) {
        return headerSupport.wrap((value) -> new StringBuilder(value).reverse().toString());
    }
```





## Use cases

### Send a request reply

`resources/application.yaml`:

```yaml
spring:
  cloud:
    stream:
      binders:
        main_session:
          type: solace
          environment:
            solace:
              java:
                host: ${SOLACE_HOSTS}
                msgVpn: ${SOLACE_MSG_VPN}
                clientUsername: ${SOLACE_USERNAME}
                clientPassword: ${SOLACE_PASSWORD}
                clientName: @project.artifactId@_${HOSTNAME}_${random.uuid}
            requestReply:
              topic: ${REQUEST_REPLY_QUEUE:replies}
              group: ${REQUEST_REPLY_GROUP_NAME:request-reply}
```

`controller/RequestReplyController.java`:

```java
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;

@Log4j2
@RestController
@RequiredArgsConstructor
public class RequestReplyController {

    private final RequestReplyService requestReplyService;

    @GetMapping(value = "/temperature/last_value/{location}")
    public SensorReading requestReplySample(
            @PathVariable("location") final String location
    ) throws ExecutionException, InterruptedException {
        SensorReading reading = new SensorReading();
        reading.setSensorID(location);

        log.info("Request " + reading);

        // Send request response and wait for answer at most for the duration as configured for the binding
        Future<SensorReading> response = requestReplyService.requestAndAwaitReply(
                reading,
                "last_value/temperature/celsius/" + location,
                SensorReading.class);

        // may throw InterruptedException if answer is not received within 2sec
        return response.get();
    }
    
}
```



### Receive and respond to a request reply

`resources/application.yaml`

```yaml
spring:
  cloud:
    function:
      definition: responseToRequest
    stream:
      bindings:
        responseToRequest-in-0:
          destination: last_value/temperature/*/*
          contentType: "application/json"
          binder: main_session

        responseToRequest-out-0:
          contentType: "application/json"
          binder: main_session

      binders:
        main_session:
          type: solace
          environment:
            solace:
              java:
                host: ${SOLACE_HOSTS}
                msgVpn: ${SOLACE_MSG_VPN}
                clientUsername: ${SOLACE_USERNAME}
                clientPassword: ${SOLACE_PASSWORD}
                clientName: @project.artifactId@_${HOSTNAME}_${random.uuid}
```

`config/PingPongConfig.java`:

```java
import ch.sbb.tms.platform.springbootstarter.requestreply.util.MessagingUtil;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class PingPongConfig {

  @Bean
  @Autowired
  public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequest(RequestReplyMessageHeaderSupportService headerSupport) {
    return headerSupport.wrap((readingIn) -> { // Add correction, target destination to response msg.
      log.info(readingIn);

      SensorReading reading = new SensorReading();
      reading.setSensorID(readingIn.getSensorID());
      reading.setTimestamp(readingIn.getTimestamp());
      reading.setBaseUnit(SensorReading.BaseUnit.CELSIUS);
      reading.setTemperature(Math.random() * 35d);

      return reading;
    });
  }
}

```



## Extensibility

The Request/Reply Spring Boot Starter has been designed to work with both the [Solace Binder](https://github.com/SolaceProducts/solace-spring-cloud) and the [TestSupportBinder](https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream-test-support/src/main/java/org/springframework/cloud/stream/test/binder/TestSupportBinder.java), but can be extended to work with other binders as well.

For that the following beans must be provided to be able to adopt to another binder.



### MessageBuilderConfigurer

The `MessageBuilderConfigurer` is responsible for providing a `RequestMessageBuilderAdapter` implementation that modifies a [MessageBuilder](https://github.com/spring-projects/spring-integration/blob/main/spring-integration-core/src/main/java/org/springframework/integration/support/MessageBuilder.java) during request message creation to properly set some message properties as expected by the particular binder implementation. For proper prioritization the bean should be annotated with the `@Order` annotation. (see [@Order in Spring @Baeldung])



The starter already includes the following MessageBuilderConfigurers:

- `SolaceMessageBuilderConfigurer` _Order: 200_
  Message builder configurer for Solace binder
- `DefaultMessageBuilderConfigurer` _Order LOWEST_PRECEDENCE_
  Fallback message builder configurer following Spring Integration / Spring Cloud standards as is used e.g. by the TestSupportBinder



### BinderConsumerPropertiesConfigurer

The `BinderConsumerPropertiesConfigurer` is responsible for mapping the properties provided through the binder environments `requestReply.consumerProperties` to [ConsumerProperties] as required by the binder. For proper prioritization the bean should be annotated with the `@Order` annotation. (see [@Order in Spring @Baeldung])



The starter already includes the following BinderConsumerPropertiesConfigurer:

- `SolaceConsumerPropertiesConfigurer` _Order: 200_
  maps to [ExtendedConsumerProperties]&lt;[SolaceConsumerProperties]&gt; as required by the Solace binder
- `DefaultConsumerPropertiesConfigurer` _Order LOWEST_PRECEDENCE_
  maps to [ConsumerProperties] as a fallback implementation and as required by the TestSupportBinder



### Message and MessageHeader parsers

When receiving messages, the application must be able to determine correlationId, destination and replyTo properties attached to a message. Unless a particular binder adheres to spring messaging standards or requires differing headers for performance optimizations, additional message parsers (or related message header parsers) must be present. For proper prioritization the bean should be annotated with the `@Order` annotation. (see [@Order in Spring @Baeldung])



The Starter includes the following message parser interfaces:

- `MessageCorrelationIdParser` - root interface for parsing the correlation id from an incoming message
  - `MessageHeaderCorrelationIdParser` - extended interface for parsing the correlation id from an incoming message's MessageHeaders
- `MessageDestinationParser` - root interface for parsing the destination from an incoming message
  - `MessageHeaderDestinationParser`- extended interface for parsing the destination from an incoming message's MessageHeaders
- `MessageReplyToParser` - root interface for parsing the reply destination from an incoming message
  - `MessageHeaderReplyToParser` - extended interface for parsing the reply destination from an incoming message's MessageHeaders



The starter also includes the following message parser implementations:

- `SolaceHeaderParser`  _Order: 200_
  implements `MessageHeaderCorrelationIdParser`,  `MessageHeaderDestinationParser`, `MessageHeaderReplyToParser` for the Solace binder
- `SpringCloudStreamHeaderParser` _Order 10000_
  implements `MessageHeaderDestinationParser` for standard Spring Cloud Stream headers
- `SpringIntegrationHeaderParser` _Order 20000_
  implements `MessageHeaderCorrelationIdParser` for standard Spring Integration headers
- `BinderHeaderParser` _Order 30000_
  implements `MessageHeaderDestinationParser` for standard Spring Cloud Stream Binder headers
- `SpringHeaderParser` _Order 40000_
  implements `MessageHeaderReplyToParser` for Spring Framework message header standards
- `HttpHeaderParser` _Order LOWEST_PRECEDENCE_
  implements `MessageHeaderCorrelationIdParser` according to HTTP header standard



## Known issues and Open Points

### Statefulness
Since message relations are kept in memory, this starter is neither fail-safe nor scalable.

More precisely:
- If one instance of the service sends a message and another one recieves the response, these can not be related.
- If a service dies, any relations are forgotten and replies can no longer be related to requests,
  potentially resulting in message loss.

## External Links
- [Spring Cloud Stream Solace Samples](https://solace.com/samples/solace-samples-spring/spring-cloud-stream/)



<!-- reused links -->

[@Order in Spring @Baeldung]: https://www.baeldung.com/spring-order
[ConsumerProperties]: https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream/src/main/java/org/springframework/cloud/stream/binder/ConsumerProperties.java
[ExtendedConsumerProperties]: https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream/src/main/java/org/springframework/cloud/stream/binder/ExtendedConsumerProperties.java
[MessageChannels]: https://docs.spring.io/spring-integration/docs/current/reference/html/core.html#spring-integration-core-messaging
[Spring Cloud Stream Binders]: https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#spring-cloud-stream-overview-binders
[SolaceConsumerProperties]: https://github.com/SolaceProducts/solace-spring-cloud/blob/master/solace-spring-cloud-stream-binder/solace-spring-cloud-stream-binder-core/src/main/java/com/solace/spring/cloud/stream/binder/properties/SolaceConsumerProperties.java
