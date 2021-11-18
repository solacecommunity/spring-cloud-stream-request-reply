# `spring-boot-starter-request-reply`
## Description
This Spring Boot Starter provides request-reply functionality for [Spring Cloud Stream Binders].

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



### Usage

#### For requester

If your ant to send a request.  
You have to define the `requestReplyReplies-in-0` binding.  
Here your need in minimum the destination as defined in your api.

```yaml
spring:
  cloud:
    function:
      definition: requestReplyReplies
    stream:
      bindings:
        requestReplyReplies-in-0:
          destination: ${REQUEST_REPLY_TOPIC:requestReply/response/@project.artifactId@_${HOSTNAME}_${random.uuid}}
```

[Full example](https://code.sbb.ch/projects/TP_TMS_PLATTFORM/repos/springcloudstream-examples/browse/pub_sub_sending/src/main/java/ch/sbb/tms/springcloudstream/examples/pubsubsending/controller/RequestReplyController.java)

#### For replier

You want to answer to a message, in general you not need this lib.
Simple response to the reply to header and reply all requested header.

Or let this do the helper methode `RequestReplyMessageHeaderSupportService.wrap` from this library.

You not need any configuration to use this.

```java
public class PingPongConfig {
  @Bean
  public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequest(RequestReplyMessageHeaderSupportService headerSupport) {
    return headerSupport.wrap((readingIn) -> {
      SensorReading reading = new SensorReading();
      ...

      return reading;
    });
  }
}
```

[Full example](https://code.sbb.ch/projects/TP_TMS_PLATTFORM/repos/springcloudstream-examples/browse/pub_sub_receiving/src/main/java/ch/sbb/tms/springcloudstream/examples/pubsubreceiving/config/PingPongConfig.java)


### API

#### `RequestReplyService`

The request/reply functionality provided by this starter can be utilized by autowiring the `RequestReplyService` which offers  the following methods:

- `requestAndAwaitReply(Q request, String requestDestination, Class<A> expectedResponseClass, Binder<?, ?, ?> binder)`
  sends the given request to the given request destination using the given binder, awaits the response and maps it to the provided class as a return value
- `requestAndAwaitReply(Q request, String requestDestination, Class<A> expectedResponseClass, String binderName)`
  sends the given request to the given request destination using the binder identified by the given name, awaits the response and maps it to the provided class as a return value

__Note:__
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

## Extensibility

The Request/Reply Spring Boot Starter has been designed to work with both the [Solace Binder](https://github.com/SolaceProducts/solace-spring-cloud) and the [TestSupportBinder](https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream-test-support/src/main/java/org/springframework/cloud/stream/test/binder/TestSupportBinder.java), but can be extended to work with other binders as well.

For that the following beans must be provided to be able to adopt to another binder.

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
- If a service dies, any relations are forgotten and replies can no longer be related to request,
  potentially resulting in message loss.

### Use for multiple binder / apis

- You can only use request reply on a single binder
- Because the reply topic global is can be only used for a single api.

## External Links
- [Spring Cloud Stream Solace Samples](https://solace.com/samples/solace-samples-spring/spring-cloud-stream/)



<!-- reused links -->

[@Order in Spring @Baeldung]: https://www.baeldung.com/spring-order
[ConsumerProperties]: https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream/src/main/java/org/springframework/cloud/stream/binder/ConsumerProperties.java
[ExtendedConsumerProperties]: https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream/src/main/java/org/springframework/cloud/stream/binder/ExtendedConsumerProperties.java
[MessageChannels]: https://docs.spring.io/spring-integration/docs/current/reference/html/core.html#spring-integration-core-messaging
[Spring Cloud Stream Binders]: https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#spring-cloud-stream-overview-binders
[SolaceConsumerProperties]: https://github.com/SolaceProducts/solace-spring-cloud/blob/master/solace-spring-cloud-stream-binder/solace-spring-cloud-stream-binder-core/src/main/java/com/solace/spring/cloud/stream/binder/properties/SolaceConsumerProperties.java
