# spring-boot-starter-request-reply
## Description
This Spring Boot Starter provides request-reply functionality for spring cloud based projects.

The code is based on the original code created in the [ESTA Spring Cloud Stream Sample](https://code.sbb.ch/projects/TP_TMS_PLATTFORM/repos/springcloudstream-examples/browse).

## Usage
### Dependencies
In order to be able to use the request/reply functionality

```xml
<dependency>
    <groupId>ch.sbb.tms.platform</groupId>
    <artifactId>spring-boot-starter-request-reply</artifactId>
</dependency>
```

### Configuration
#### Sender / Receiver
The Request/Reply Spring Boot Starter can be used both in requesting and replying services.

If used in requesting services, a default Solace Binder must be defined within property `spring.cloud.stream.requestreply.binderName`
in order for the `RequestReplyService` to be created and queues and exchanges to be initialized.
In the following example the default Spring Cloud Stream binder is used:

```yaml
spring:
  cloud:
    stream:
	  requestreply:
	    binderName: ${spring.cloud.stream.defaultBinder}
```

If omitted, the library can still be used to offer receiving services helper methods to wrap response functions by means of the MessagingUtil, which then can be picked up through Spring Cloud Functions. e.g.:

```java
    @Bean
    public Function<Message<String>, Message<String>> reverse() {
        return MessagingUtil.wrap((value) -> new StringBuilder(value).reverse().toString());
    }
```

#### Queues and Exchanges
When creating the `RequestReplyService` (see above) the Spring Boot Starter creates both a topic and queue in Solace to receive responses on.
These are equally named as defined through the environment variable `REQUEST_REPLY_QUEUE` and defaulting to `replies`. It is also possible to
override the value by setting `spring.cloud.stream.requestreply.replyToQueueName` accordingly.

All request/reply consumers are grouped with the name defined in environment variable `REQUEST_REPLY_GROUP_NAME` which defaults to `request-reply`.
It is also possible to override the value by setting `spring.cloud.stream.requestreply.requestReplyGroupName` accordingly.

Furthermore a Solace Binder must be defined for the `RequestReplyService` and queue and exchange to be created by populating
`spring.cloud.stream.requestreply.binderName`. e.g.:

```yaml
spring:
  cloud:
    stream:
	  requestreply:
	    binderName: ${spring.cloud.stream.defaultBinder}
		
      defaultBinder: main_session

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
                clientName: ${HOSTNAME}_${random.uuid}
                connectRetries: 5
                reconnectRetries: 3
                connectRetriesPerHost: 2
                reconnectRetryWaitInMillis: 3000
                apiProperties:
                  SUB_ACK_WINDOW_SIZE: 255
                  PUB_ACK_WINDOW_SIZE: 255

      solace:
        bindings:
          logger-in-0:
            consumer:
              provisionDurableQueue: true
              queueRespectsMsgTtl: true
```

### API
The request/reply functionality provided by this starter can be utilized by autowiring
the `RequestReplyService` which offers  the following methods:

- `RequestReplyService.requestAndAwaitReply(Q request, String requestDestination, long timeOutInMs, Class<A> expectedClass)`
  sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
- `RequestReplyService.requestReply(Q request, String requestDestination, String replyToDestination, long timeOutInMs)`
  sends the given request to the given message channel and prepares the framework to await the response within the given time frame to send them back via the provided reply destination
- `RequestReplyService.requestReply(Q request, String requestDestination, Consumer<Message<?>> responseConsumer, long timeOutInMs`
  sends the given request to the given message channel and prepares the framework to await the response within the given time frame to direct them at the consumer provided

__Note:__
- the `RequestReplyService` uses the spring integration argument resolver message converter
  to convert between the data types of different message channels (see [Payload Type Conversion @ Spring Integration Documentation](https://docs.spring.io/spring-integration/docs/5.1.4.RELEASE/reference/html/#payload-type-conversion)).
  In order to create custom converters, have a look at [Guide to Spring Type Conversions @ Baeldung](https://www.baeldung.com/spring-type-conversions) .
- request channels that receive requests must pay respect to the correlation id set in either `IntegrationMessageHeaderAccessor.CORRELATION_ID` or `SolaceHeaders.CORRELATION_ID` to create respective responses<br/>
  if you implement such functions on your own, you might use `MessagingUtil` to wrap them
- request channels that receive requests must pay respect to the reply channel set in either `MessageHeaders.REPLY_CHANNEL` or `SolaceHeaders.REPLY_TO` to direct respective responses to<br/>
  if you implement such functions on your own, you might use `MessagingUtil` to wrap them

## Use cases

### Send a request reply

resources/application.yaml
```yaml
spring:
  cloud:
    stream:
      requestreply:
        replyToQueueName: ${REQUEST_REPLY_QUEUE:replies}
        requestReplyGroupName: ${REQUEST_REPLY_GROUP_NAME:request-reply}
        binderName: main_session

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

controller/RequestReplyController.java
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

        // Send request response and wait for answer for max 2sec for answer.
        Future<SensorReading> response = requestReplyService.requestAndAwaitReply(
                reading,
                "last_value/temperature/celsius/" + location,
                2000,
                SensorReading.class);

        // may throw InterruptedException if answer is not received within 2sec
        return response.get();
    }
    
}
```

### Receive and respond to a request reply

resources/application.yaml
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

config/PingPongConfig.java
```java
import ch.sbb.tms.platform.springbootstarter.requestreply.util.MessagingUtil;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class PingPongConfig {

  @Bean
  public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequest() {
    return MessagingUtil.wrap((readingIn) -> { // Add correction, target destination to response msg.
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

## Known issues and Open Points
### Statefulness
Since message relations are kept in memory, this starter is neither fail-safe nor scalable.

More precisely:
- If one instance of the service sends a message and another one recieves the response, these can not be related.
- If a service dies, any relations are forgotten and replies can no longer be related to requests,
  potentially resulting in message loss.

## External Links
- [Spring Cloud Stream Solace Samples](https://solace.com/samples/solace-samples-spring/spring-cloud-stream/)

