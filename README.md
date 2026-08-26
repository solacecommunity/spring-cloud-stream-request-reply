# `spring-cloud-stream-starter-request-reply`

## Description

This Spring Boot starter adds request-reply support to
[Spring Cloud Stream binders](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binders.html).

## Spring Cloud Version Compatibility

Use the table below to pick the version you need:

| Spring Cloud | spring-cloud-stream-starter-request-reply | Spring Boot | sol-jcsmp |
|--------------|-------------------------------------------|-------------|-----------|
| 2025.1.2     | 6.1.1                                     | 4.1.0       | 10.30.1   |
| 2025.1.2     | 6.1.0                                     | 4.1.0       | 10.30.1   |
| 2025.1.1     | 6.0.1                                     | 4.0.5       | 10.29.0   |
| 2025.1.1     | 6.0.0                                     | 4.0.2       | 10.29.0   |
| 2025.0.0     | 5.3.5                                     | 3.5.8       | 10.29.0   |
| 2025.0.0     | 5.3.4                                     | 3.5.8       | 10.29.0   |
| 2025.0.0     | 5.3.3                                     | 3.5.8       | 10.29.0   |
| 2025.0.0     | 5.3.2                                     | 3.5.8       | 10.29.0   |
| 2025.0.0     | 5.3.1                                     | 3.5.6       | 10.28.1   |
| 2025.0.0     | 5.2.4, 5.3.0                              | 3.5.4       | 10.27.3   |
| 2024.0.0     | 5.2.3                                     | 3.4.2       | 10.25.2   |
| 2024.0.0     | 5.2.2                                     | 3.4.2       | 10.25.2   |
| 2023.0.2     | 5.1.5                                     | 3.3.0       | 10.24.0   |
| 2023.0.2     | 5.1.4                                     | 3.3.0       | 10.24.0   |
| 2023.0.2     | 5.1.3                                     | 3.3.0       | 10.23.0   |
| 2023.0.1     | 5.1.2                                     | 3.2.5       | 10.23.0   |

## Usage

### Dependency

To enable the request-reply functionality, add this dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>community.solace.spring.cloud</groupId>
    <artifactId>spring-cloud-stream-starter-request-reply</artifactId>
    <version>6.1.1</version>
</dependency>
```

### Requester Side

To send a request, you need a binding and a topic pattern that matches the topic you send your requests to.
The binding defines the binder, the content type and the reply address where the replier sends its response.

`spring.cloud.stream.requestreply.bindingMapping[n].binding` must:

- match an entry in `spring.cloud.function.definition`, and
- match `spring.cloud.stream.bindings.XX-in-0`, where you define the binder, the content type and so on.

`spring.cloud.stream.requestreply.bindingMapping[n].topicPatterns[m]`:

- is a list of regular expressions that are matched against the destination of your requests.
- If no pattern matches when you call `requestAndAwaitReplyToTopic()` or `requestReplyToTopic()`,
  an `IllegalArgumentException` is thrown.
- You do not need this setting if you only use `requestAndAwaitReplyToBinding()` or `requestReplyToBinding()`.

Remember to list this binding in `spring.cloud.function.definition` as well.
Otherwise you never receive a response.
You do not need to write a bean for it, because the library creates it for you.

#### Using Dynamic Topics

```yaml
spring:
  cloud:
    function:
      definition: requestReplyRepliesDemo
    stream:
      requestreply:
        bindingMapping:
          - binding: requestReplyRepliesDemo
            replyTopic: requestReply/response/solace/{StagePlaceholder}/@project.artifactId@_${HOSTNAME}_${replyTopicWithWildcards|uuid}
            topicPatterns:
              - requestReply/request/.*
      bindings:
        requestReplyRepliesDemo-in-0:
          destination: ${replyTopicWithWildcards|requestReplyRepliesDemo|*}
          contentType: "application/json"
          binder: solace
        requestReplyRepliesDemo-out-0:
          binder: solace
```

##### Single Response

```java
        SensorReading response = requestReplyService.requestAndAwaitReplyToTopic(
                reading,
                "requestReply/request/last_value/temperature/celsius/" + location,
                SensorReading.class,
                Duration.ofSeconds(30)
        );
```

##### Multiple Responses

New to reactive streams? Start with this
[introduction to Flux and Project Reactor](https://www.baeldung.com/reactor-core).

```java
        Flux<SensorReading> responses = requestReplyService.requestReplyToTopicReactive(
                reading,
                "requestReply/request/last_value/temperature/celsius/" + location,
                SensorReading.class,
                Duration.ofSeconds(30)
        );
```

#### Using Static Topics

```yaml
spring:
  cloud:
    function:
      definition: requestReplyRepliesDemo
    stream:
      requestreply:
        bindingMapping:
          - binding: requestReplyRepliesDemo
            replyTopic: requestReply/response/solace/{StagePlaceholder}/@project.artifactId@_${HOSTNAME}_${replyTopicWithWildcards|uuid}
      bindings:
        requestReplyRepliesDemo-in-0:
          destination: ${replyTopicWithWildcards|requestReplyRepliesDemo|*}
          contentType: "application/json"
          binder: solace
        requestReplyRepliesDemo-out-0:
          destination: requestReply/request/last_value/temperature/celsius/livingroom
          binder: solace
```

##### Single Response

```java
        SensorReading response = requestReplyService.requestAndAwaitReplyToBinding(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofSeconds(30)
        );
```

##### Multiple Responses

New to reactive streams? Start with this
[introduction to Flux and Project Reactor](https://www.baeldung.com/reactor-core).

```java
        Flux<SensorReading> responses = requestReplyService.requestReplyToBindingReactive(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofSeconds(30)
        );
```

[Full example](examples/request_reply_sending/src/main/java/community/solace/spring/cloud/requestreply/examples/sending/controller/RequestReplyController.java)

#### How Everything Fits Together

When you call `requestAndAwaitReplyToTopic()` or `requestReplyToTopic()`,
the library matches the topic from your code against every
`spring.cloud.stream.requestreply.bindingMapping[].topicPatterns`.
The first hit wins.

![topic to pattern](doc/requester_topic_to_pattern.png)

The `spring.cloud.stream.requestreply.bindingMapping[].binding` of the matching section is looked up in
`spring.cloud.stream.bindings[]`, and the library always uses the `-out-0` entry.
This tells the request-reply service which `binder`, `contentType` and so on to use.

![binding to -out-0](doc/binding_to_out-0.png)

The `spring.cloud.stream.requestreply.bindingMapping[].replyTopic` of the matching section goes into the outgoing
message. It tells the other service where you expect the answer.

This topic should be unique per process. As a best practice, put the following into it:

- The `HOSTNAME`, to make debugging a little easier.
- A UUID, so that your inbox topic is unique.
  Do not use Spring's `${random.uuid}`, because it creates a new UUID on every call.
  Use `${replyTopicWithWildcards|uuid}` instead. It gives you one fixed UUID, created when the process starts.

![reply topic sending](doc/reply_topic_sending.png)

The request-reply service walks through `spring.cloud.stream.requestreply.bindingMapping`
and creates a bean that consumes the messages arriving on `-in-0.destination`, using the configured binder.

![consuming topic](doc/consuming_topic.png)

As soon as you use the `{StagePlaceholder}` feature, you can no longer listen on a fixed topic such as:

```
requestReply/response/solace/{StagePlaceholder}/pub_sub_sending_K353456_315fd96b-b981-417b-be99-3be065c6611d
```

The reason is that the other side replaces `{StagePlaceholder}` before it answers, for example with `p-pineapple`.
So you have to listen on:

```
requestReply/response/solace/*/pub_sub_sending_K353456_315fd96b-b981-417b-be99-3be065c6611d
```

`${replyTopicWithWildcards|requestReplyRepliesDemo|*}` does that for you.
It takes the `replyTopic` of the `bindingMapping` section named by the first parameter
and replaces every `{someThing}` with the wildcard given as the second parameter, here `*`.

![reply topic replace wildcard](doc/replyTopicWithWildcards.png)

### Replier Side

You do not need this library just to answer a message.
You can also send the response to the topic from the reply-to header yourself
and copy all headers from the request to the response.

Still, the methods `RequestReplyMessageHeaderSupportService.wrap`,
`RequestReplyMessageHeaderSupportService.wrapList` and `RequestReplyMessageHeaderSupportService.wrapFlux`
help you build that response: they set the message headers for you and resolve variables in dynamic topics.

By default, the wrapping methods set the correlation ID and the reply destination header.
For multi-response replies, they also set the `totalReplies` and `replyIndex` headers.
To copy further headers from the request, list them in `spring.cloud.stream.requestreply.copyHeadersOnWrap`:

```properties
spring.cloud.stream.requestreply.copyHeadersOnWrap=encoding,yetAnotherHeader
```

The examples below show how to use the wrapping methods.

#### Single Response

```java
public class PingPongConfig {
  @Bean
  public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequest(
          RequestReplyMessageHeaderSupportService headerSupport
  ) {
    return headerSupport.wrap((request) -> {
      SensorReading response = new SensorReading();
      response.setFoo(1337);

      return response;
    });
  }
}
```

[Full example](examples/request_reply_response/src/main/java/community/solace/spring/cloud/requestreply/examples/response/config/PingPongConfig.java)

#### Multiple Responses, Functional

Use this style when you know all responses up front.

```java
public class PingPongConfig {
  @Bean
  public Function<Message<SensorRequest>, List<Message<SensorReading>>> responseMultiToRequestKnownSizeSolace(
          RequestReplyMessageHeaderSupportService headerSupport
  ) {
    return headerSupport.wrapList((request) -> {
      List<SensorReading> responses = new ArrayList<>();
      responses.add(new SensorReading());
      // ....

      return responses;
    }, "responseMultiToRequestKnownSizeSolace-out-0");
  }
}
```

[Full example](examples/request_reply_response/src/main/java/community/solace/spring/cloud/requestreply/examples/response/config/PingMultiPongConfig.java)

#### Multiple Responses, Reactive

Use this style when you do not know in advance how many responses there will be.

```java
public class PingPongConfig {
  @Bean
  public Function<Flux<Message<SensorRequest>>, Flux<Message<SensorReading>>> responseMultiToRequestRandomSizeSolace(
          RequestReplyMessageHeaderSupportService headerSupport
  ) {
    return headerSupport.wrapFlux((request, responseSink) -> {
      try {
        while (yourBusinessLogic) { // Your business logic can push 0 to N responses.
          responseSink.next(response);
        }
        responseSink.complete();
      } catch (Exception e) {
        responseSink.error(new IllegalArgumentException("Business error message", e));
      }
    }, "responseMultiToRequestRandomSizeSolace-out-0");
  }
}
```

[Full example](examples/request_reply_response/src/main/java/community/solace/spring/cloud/requestreply/examples/response/config/PingMultiPongConfig.java)

#### Error Handling

You may want to forward errors to the requester.
To do so, pass one or more exception classes to the wrapping method.
Only these exceptions are sent back:

```java
public class PingPongConfig {
  @Bean
  public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequest(
          RequestReplyMessageHeaderSupportService headerSupport
  ) {
    return headerSupport.wrap((request) -> {
      SensorReading response = new SensorReading();
      response.setFoo(1337);

      return response;
    }, MyBusinessException.class, SomeOtherException.class);
  }
}
```

Bean validation errors and JSON parsing errors cannot be returned to the requester out of the box.
You have to run that validation yourself, for example:

```java
public class PingPongConfig {
  @Qualifier("mvcValidator")
  private final Validator validator;

  private final ObjectMapper objectMapper;

  @Bean
  public Function<Message<String>, Message<SensorReading>> responseToRequest(
          RequestReplyMessageHeaderSupportService headerSupport
  ) {
    return headerSupport.wrap((rawRequest) -> {
      SensorReading request = objectMapper.readValue(rawRequest.getPayload(), SensorReading.class);
      final DataBinder db = new DataBinder(request);
      db.setValidator(validator);
      db.validate();


      SensorReading response = new SensorReading();
      response.setFoo(1337);

      return response;
    }, MyBusinessException.class, SomeOtherException.class);
  }
}
```

#### Variable Replacement

The requester may put placeholders into the reply destination.
You have to replace them before you send the response.

This is useful when several instances can answer a request, for example for load balancing
or for redundancy across data centers. Knowing which instance answered makes debugging easier.

For example, the reply destination header of the request may contain the placeholder `{StagePlaceholder}`.
The configuration below replaces that placeholder with a string that identifies the instance:

```yaml
spring:
  cloud:
    function:
      definition: requestReplyRepliesDemo
    stream:
      requestreply:
        variableReplacements:
          "{StagePlaceholder}": ${RCS_ENV_ROLE}-${RCS_CLUSTER}
```

These `variableReplacements` are applied to request topics and to reply topics.

#### Custom Logging

If the built-in logging does not fit your needs, define your own logging bean:

```java
@Configuration
public class CustomizedLoggerConfig {

    @Bean
    public RequestReplyLogger requestReplyLogger() {
        return new CustomizedLogger();
    }
}
```

An example implementation of the logger interface:

```java
public class CustomizedLogger implements RequestReplyLogger {

    @Override
    public void logRequest(Logger logger, Level suggestedLevel, String suggestedLogMessage, Message<?> message) {
        logger.atLevel(Level.DEBUG).log("<<< {} {}", message.getPayload(), message.getHeaders());
    }

    @Override
    public void logReply(Logger logger, Level suggestedLevel, String suggestedLogMessage, long remainingReplies, Message<?> message) {
        String payloadString = new String((byte[])message.getPayload());
        logger.atLevel(Level.DEBUG).log(">>> {} {} remaining replies: {}", payloadString, message.getHeaders(), remainingReplies);
    }

    @Override
    public void log(Logger logger, Level suggestedLevel, String suggestedLogMessage, Object... formatArgs) {
        logger.atLevel(suggestedLevel).log(suggestedLogMessage, formatArgs);
    }
}
```

A complete example application is available under `examples/customized_logging`.

### Custom Message Interception

On the requester side, define a bean of type `RequestSendingInterceptor` to change a request message
before it is sent. A complete example is available under `examples/customized_reply_to_header_sending`.

On the replier side, define a bean of type `ReplyWrappingInterceptor` to change a message while it is wrapped.
A complete example is available under `examples/customized_reply_to_header_response`.

### API

#### `RequestReplyService`

Autowire `RequestReplyService` to use the request-reply functionality. It offers the following methods.

##### For a Single Response

Use these methods when you expect exactly one response.

- `A requestAndAwaitReplyToTopic(Q request, String requestDestination, Class<A> expectedResponseClass, Duration timeoutPeriod)`
  sends the request to the given destination, waits for the response and maps it to the given class.
  A configured `-out-0.destination` is ignored.

- `A requestAndAwaitReplyToBinding(Q request, String bindingName, Class<A> expectedResponseClass, Duration timeoutPeriod)`
  sends the request to the destination configured for the `-out-0` of this binding,
  waits for the response and maps it to the given class.

- `CompletableFuture<A> requestReplyToTopic(Q request, String requestDestination, Class<A> expectedClass, Duration timeoutPeriod)`
  sends the request to the given destination.
  It returns a future that maps the response to the given class.
  A configured `-out-0.destination` is ignored.
  Use this method only in rare cases, when you need several request-reply calls to run in parallel on the same thread.

- `CompletableFuture<A> requestReplyToBinding(Q request, String bindingName, Class<A> expectedClass, Duration timeoutPeriod)`
  sends the request to the destination configured for the `-out-0` of this binding.
  It returns a future that maps the response to the given class.
  Use this method only in rare cases, when you need several request-reply calls to run in parallel on the same thread.

##### For Multiple Responses

Use these methods when you expect zero to N responses.

- `Flux<A> requestReplyToTopicReactive(Q request, String requestDestination, Class<A> expectedClass, Duration timeoutPeriod)`
  sends the request to the given destination.
  It returns a reactive stream that maps the responses to the given class.
  If your response type is an array, send the elements as separate messages.
  This keeps you below the message size limit.

- `Flux<A> requestReplyToBindingReactive(Q request, String bindingName, Class<A> expectedClass, Duration timeoutPeriod)`
  sends the request to the destination configured for the `-out-0` of this binding.
  It returns a reactive stream that maps the responses to the given class.
  If your response type is an array, send the elements as separate messages.
  This keeps you below the message size limit.

##### Example: Blocking

A blocking request-reply call that returns a list of answers.

```java
    @GetMapping(value = "/temperature/last_hour/{location}")
    public List<SensorReading> requestMultiReplySample(
            @PathVariable("location") final String location
    ) {
        MyRequest request = new MyRequest();
        request.setLocation(location);

        return requestReplyService.requestReplyToTopicReactive(
                        request,
                        "last_hour/temperature/celsius/" + location,
                        SensorReading.class,
                        Duration.ofSeconds(30)
                )
                .collectList()
                .block();
    }
```

##### Example: Non-Blocking

The same call, but the calling thread is not blocked. Every answer is handled as soon as it arrives.

```java
    @GetMapping(value = "/temperature/last_hour/{location}")
    public void requestMultiReplySample(
            @PathVariable("location") final String location
    ) {
        MyRequest request = new MyRequest();
        request.setLocation(location);

        requestReplyService.requestReplyToTopicReactive(
                        request,
                        "last_hour/temperature/celsius/" + location,
                        SensorReading.class,
                        Duration.ofSeconds(30)
                )
                .subscribe(
                        sensorReading -> log.info("Got an answer: " + sensorReading),
                        throwable -> log.error("The request was finished with error", throwable),
                        () -> log.info("The request was finished")
                );
    }
```

##### Receiving Many Answers

If your request is an `org.springframework.messaging.Message`, you decide whether every response travels
as its own message or whether several responses are grouped into one message.

Turn grouping on with the `groupedMessages` header:

```java
Message<MyRequest> requestMsg = MessageBuilder.withPayload(request)
        .setHeader(SpringHeaderParser.GROUPED_MESSAGES, true)
        .build();
```

If your request is not a `Message`, the library sets `groupedMessages=true` for you.

Unless you need separate headers per reply, prefer grouped messages.
Grouping makes replies faster, because it saves message header overhead and broker resources.

Messages are grouped until one of these limits is reached:

- The grouped message grows beyond 1 MB.
- The group holds 10,000 individual messages.
- The first message of the group is older than 200 ms.
  The replier can configure a different timeout in `wrapFlux`.

#### `RequestReplyMessageHeaderSupportService`

A service that only answers requests can still use this library.
It provides helper methods that wrap your response function and set the destination header of the message.
Spring Cloud Function then routes the response for you. For example:

```java
    @Bean
    public Function<Message<String>, Message<String>> reverse(RequestReplyMessageHeaderSupportService headerSupport) {
        return headerSupport.wrap((value) -> new StringBuilder(value).reverse().toString());
    }
```

Return `null` from the wrapped function to drop the message.

## Extensibility

This starter works with the [Solace binder](https://github.com/SchweizerischeBundesbahnen/spring-cloud-stream-binder)
and with the [TestSupportBinder](https://github.com/spring-cloud/spring-cloud-stream/blob/main/spring-cloud-stream-test-support/src/main/java/org/springframework/cloud/stream/test/binder/TestSupportBinder.java).
You can extend it for other binders by providing the beans described below.

### Message and Message Header Parsers

For every incoming message, the library needs the correlation ID, the destination and the reply-to property.
A binder that does not follow the Spring messaging standards, or that uses different headers for performance
reasons, needs its own message parsers or message header parsers.
Annotate the bean with `@Order` to control its priority
(see [@Order in Spring at Baeldung](https://www.baeldung.com/spring-order)).

The starter ships these parser interfaces:

- `MessageCorrelationIdParser` — root interface, reads the correlation ID from an incoming message
  - `MessageHeaderCorrelationIdParser` — reads the correlation ID from the message's `MessageHeaders`
- `MessageDestinationParser` — root interface, reads the destination from an incoming message
  - `MessageHeaderDestinationParser` — reads the destination from the message's `MessageHeaders`
- `MessageReplyToParser` — root interface, reads the reply destination from an incoming message
  - `MessageHeaderReplyToParser` — reads the reply destination from the message's `MessageHeaders`
- `MessageTotalRepliesParser` — root interface, reads the total number of replies from an incoming multi-response message
  - `MessageHeaderTotalRepliesParser` — reads the total number of replies from the message's `MessageHeaders`

It also ships these implementations:

- `SolaceHeaderParser` _(order 200)_
  implements `MessageHeaderCorrelationIdParser`, `MessageHeaderDestinationParser` and `MessageHeaderReplyToParser`
  for the Solace binder.
- `SpringCloudStreamHeaderParser` _(order 10000)_
  implements `MessageHeaderDestinationParser` and `MessageTotalRepliesParser` for standard Spring Cloud Stream headers.
- `SpringIntegrationHeaderParser` _(order 20000)_
  implements `MessageHeaderCorrelationIdParser` for standard Spring Integration headers.
- `BinderHeaderParser` _(order 30000)_
  implements `MessageHeaderDestinationParser` for standard Spring Cloud Stream binder headers.
- `SpringHeaderParser` _(order 40000)_
  implements `MessageHeaderReplyToParser` for Spring Framework message header standards.
- `HttpHeaderParser` _(order `LOWEST_PRECEDENCE`)_
  implements `MessageHeaderCorrelationIdParser` for the HTTP header standard.

## Compatibility

### Tracing

The library forwards the Micrometer trace ID, so the spans of requester and replier end up in the same trace.

No extra configuration is needed. Just set up tracing as usual:

```yaml
spring:
  application:
    name: the-name-of-your-micro-service
management:
    zipkin:
        tracing:
            endpoint: https://demo-zipkin.xxxx.net/api/v2/spans
            export:
                enabled: true
    tracing:
        sampling:
            probability: 1.0
logging:
    pattern: correlation=[${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

#### Context Propagation Across Asynchronous Stages

A request-reply call runs on a dedicated executor. The request is sent and the reply is awaited on a different
thread than the caller. To keep tracing and other thread-local context (such as the SLF4J `MDC`) consistent,
the library propagates the
[Micrometer context](https://docs.micrometer.io/context-propagation/reference/) from the calling thread
to that executor.

The library wraps the executor itself, not each single task. Therefore the context is restored for **every**
stage of the internal pipeline, and also for stages that your application chains onto the returned
`CompletableFuture`:

```java
MDC.put("traceId", "abc");

requestReplyService
        .requestReplyToTopic(request, topic, Response.class, Duration.ofSeconds(10))
        .thenApply(response -> {
            // MDC.get("traceId") is still "abc" here, even though this runs on an executor thread
            return enrich(response);
        });
```

You do not need to capture a `ContextSnapshot` yourself. As always with Micrometer context propagation,
the matching `ThreadLocalAccessor` must be registered on the `ContextRegistry`.
For the `MDC`, your observability or tracing setup normally registers it.

### Excluding the Starter in Tests

Tests that do not need request-reply, for example a `@JsonTest` or a `@WebMvcTest` slice,
do not load the auto-configuration of this starter. There it is inactive by default.

In a wider test that does load auto-configuration, exclude it like any other auto-configuration:

```java
@SpringBootTest
@ImportAutoConfiguration(exclude = RequestReplyAutoConfiguration.class)
class MyTest {
    // ...
}
```

Or through configuration:

```yaml
spring:
  autoconfigure:
    exclude: community.solace.spring.cloud.requestreply.service.RequestReplyAutoConfiguration
```

When excluded, neither the request-reply service nor the reply consumers of the bindings are registered.

## Known Issues and Open Points

### Statefulness

The starter keeps the relation between request and reply in memory. It is therefore neither fail-safe nor scalable.

In detail:

- If one instance of the service sends a request and another instance receives the response,
  the two cannot be related.
- If a service dies, all relations are lost. Replies can no longer be matched to their request,
  which can mean lost messages.

### Reply Duplication and Requester-Side Deduplication

In some situations, for example a Solace in-place broker update with a short disconnect and reconnect,
the same request is sent twice. The replier then produces **duplicate replies**.

To stay robust against such duplicates, the requester keeps bookkeeping per request
and **deduplicates incoming replies by `replyIndex`**:

- If several messages arrive with the same `replyIndex`, only the first one is processed. Later ones are ignored.
- This also works for range indices such as `replyIndex="0-45"`, which are used when replies are grouped into an
  SDTStream. The whole grouped message is consumed only once.
- Terminal messages (finish and error) are always processed, even if they share a `replyIndex` with another message.

#### Deduplication Bitmap Size Limit for Unknown `totalReplies`

When `totalReplies` is not known yet, for example in streaming replies of unknown size,
the requester still deduplicates numeric `replyIndex` values. But it has to limit how large the internal
bitmap can grow.

Configure this limit with:

```properties
spring.cloud.stream.requestreply.dedup.maxBitsWhenUnknown=100000
```

- Default: **100000** bits.
- Effect: a `replyIndex` (or range end) above this limit is not deduplicated.

#### Example Log Message

```
2023-10-04 10:00:00.000  INFO 12345 --- [nio-8080-exec-1] c.s.s.requestreply.examples.sending     : <<< MyRequest(location=livingroom) [correlationId=12345, replyTo=requestReply/response/solace/*/pub_sub_sending_K353456_315fd96b-b981-417b-be99-3be065c6611d, ...]
2023-10-04 10:00:00.000  INFO 12345 --- [nio-8080-exec-1] c.s.s.requestreply.examples.sending     : >>> SensorReading(foo=1337) [correlationId=12345, replyTo=requestReply/response/solace/*/pub_sub_sending_K353456_315fd96b-b981-417b-be99-3be065c6611d, remainingReplies=0, ...]
```
