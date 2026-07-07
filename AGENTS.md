# AGENTS.md

Coding rules and project conventions for AI agents and human contributors working on
**spring-cloud-stream-starter-request-reply** (artifactId `spring-cloud-stream-starter-request-reply`,
groupId `community.solace.spring.cloud`). This file applies to this repository only.

The library adds synchronous request/reply and request/multi‑reply semantics on top of Spring Cloud
Stream, primarily for the Solace binder. When in doubt about behaviour, read the source under
`src/main/java/community/solace/spring/cloud/requestreply` — it is the source of truth, not this file.

## 1. Technology stack

- **Language:** Java 17 (`<java.version>17</java.version>`). Do not use language features beyond Java 17.
- **Build:** Maven, with `spring-boot-starter-parent` as the parent POM. It is a **library / Spring Boot
  starter**, not an application: the `spring-boot-maven-plugin` repackage goal is disabled and the POM is
  flattened for publishing.
- **Frameworks:** Spring Boot, Spring Cloud Stream, Spring Cloud Function, Spring Integration, Spring
  Messaging.
- **Reactive:** Project Reactor (`Flux`, `Mono`, `FluxSink`) for multi‑reply streaming.
- **Observability:** Micrometer (`micrometer-core` for metrics, `context-propagation` for tracing/MDC).
- **Solace:** Solace `sol-jcsmp` types (`SDTStream`, `Destination`) are used **optionally** — the Solace
  header parser is only registered when `com.solacesystems.jcsmp.Destination` is on the classpath.
- **Testing:** JUnit 5, Spring Boot Test, `spring-cloud-stream-test-binder`, Mockito, Reactor Test,
  Awaitility.
- Do not add heavyweight dependencies. Solace types must stay optional so the library keeps working with
  other binders (e.g. the test binder).

## 2. Project structure

```
src/main/java/community/solace/spring/cloud/requestreply/
├── service/
│   ├── RequestReplyService.java            # public API interface (requester side)
│   ├── RequestReplyServiceImpl.java        # requester implementation (correlation, timeout, executor)
│   ├── ResponseHandler.java                # per-request bookkeeping, dedup, completion latch
│   ├── RequestReplyFunctionRegistrar.java  # registers a reply consumer per bindingMapping
│   ├── RequestReplyAutoConfiguration.java  # main auto-configuration
│   ├── MessageConverter.java
│   ├── header/
│   │   ├── RequestReplyMessageHeaderSupportService.java  # responder-side wrap/wrapList/wrapFlux helpers
│   │   └── parser/                         # ordered Message/MessageHeaders parsers (correlationId,
│   │       │                               #   destination, replyTo, replyIndex, totalReplies, errorMessage)
│   │       ├── SolaceHeaderParser.java   SpringHeaderParser.java   HttpHeaderParser.java   ...
│   ├── logging/                            # RequestReplyLogger + DefaultRequestReplyLogger
│   └── messageinterceptor/                 # RequestSendingInterceptor, ReplyWrappingInterceptor
├── config/
│   ├── RequestReplyProperties.java         # @ConfigurationProperties("spring.cloud.stream.requestreply")
│   ├── BinderMappings.java                 # a single bindingMapping entry
│   ├── logging/LoggerAutoConfiguration.java
│   └── messageinterceptor/                 # Noop*Interceptor beans + MessageInterceptorAutoConfiguration
├── env/                                    # ${replyTopicWithWildcards|...} property source
├── util/                                   # MessageChunker, CheckedExceptionWrapper
└── exception/                              # RequestReplyException
src/main/resources/META-INF/
├── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports  # auto-config registration
└── spring.factories                        # EnvironmentPostProcessor registration
examples/                                   # standalone runnable example apps (own POMs)
doc/                                        # protocol diagrams and AsyncAPI spec
```

- Auto‑configuration classes are registered in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Any new
  auto‑configuration must be added there (never rely on component scan alone from a starter).
- The `${replyTopicWithWildcards|...}` `EnvironmentPostProcessor` is registered in `spring.factories`.
- Modules under `examples/` are independent Maven projects; keep them compiling but do not couple library
  code to them.

## 3. Coding standards & conventions

Formatting is defined in `.editorconfig` (IntelliJ scheme). Key rules:

- **Indent:** 4 spaces, no tabs. **Line ending:** LF. **Encoding:** UTF‑8. Trailing whitespace trimmed.
  Files do **not** end with a final newline (`insert_final_newline = false`).
- **Import order:** `java.**`, then `javax.**`, then everything else, then `org.springframework.**`, then
  static imports (`$*`), each group separated by a blank line. No wildcard/on‑demand imports (threshold
  999) — use single‑class imports.
- **Braces:** always use braces, even for single‑statement `if`/`for`/`while` (`*_brace_force = always`).
- **Naming:** implementation classes end in `Impl` (e.g. `RequestReplyServiceImpl`); test classes end in
  `Tests`.
- Prefer `@Nullable` (Spring) on nullable returns/params, as existing parser code does.
- Public API methods carry Javadoc; keep it accurate when you change signatures or behaviour.
- Do not introduce Lombok in `src/main` (it is not a main dependency here; the examples use it, main code
  does not).
- Update `CHANGELOG.md` (Keep a Changelog format, semantic versioning) for any user‑visible change.

## 4. Request/reply model & public API contract

The two public entry points are `RequestReplyService` (requester) and
`RequestReplyMessageHeaderSupportService` (responder). Treat their signatures and semantics as a stable
contract; breaking changes require a major version bump and a CHANGELOG entry.

**Requester (`RequestReplyService`):** every method is generic in request `Q` and response `A`, takes an
`expectedClass` and a `Duration timeoutPeriod`, and has an overload accepting
`Map<String, Object> additionalHeaders`. Flavours:

- `requestAndAwaitReplyTo{Topic,Binding}` → blocking, returns `A`, declares `InterruptedException`,
  `TimeoutException`, `RemoteErrorException`.
- `requestReplyTo{Topic,Binding}` → non‑blocking, returns `CompletableFuture<A>` (single response).
- `requestReplyTo{Topic,Binding}Reactive` → returns `Flux<A>` (zero to N responses).

`…Topic` variants resolve the binding by matching the destination against
`bindingMapping[].topicPatterns` (first match wins; no match → `IllegalArgumentException`). `…Binding`
variants send to the binding's `-out-0` destination.

**Responder (`RequestReplyMessageHeaderSupportService`):**

- `wrap(fn, applicationExceptions...)` → single `Message` response. Returning `null` drops the message.
- `wrapList(fn, bindingName, applicationExceptions...)` → `List<Message>` of known size.
- `wrapFlux(fn, bindingName[, groupTimeout])` → streaming `Flux<Message>` of unknown size.

Invariants the wrappers must preserve:

- copy the **correlationId** onto every reply,
- set the reply **destination** (`BinderHeaders.TARGET_DESTINATION`) from the request's reply‑to header,
  applying `variableReplacements`,
- set `totalReplies` and `replyIndex` headers for multi‑reply, and always emit a terminal (empty) message
  to close the stream,
- forward a matching `applicationException` as an error reply (`errorMessage` header) which the requester
  surfaces as `RemoteErrorException` (or an errored `Flux`),
- copy any headers listed in `copyHeadersOnWrap`.

**Reply routing:** for each `bindingMapping`, `RequestReplyFunctionRegistrar` registers a
`Consumer<Message<?>>` bean named after the binding, wired to `-in-0`, delegating to
`RequestReplyServiceImpl.onReplyReceived`. Never register this consumer in a way that runs in sliced test
contexts (see §6). If a bean with the binding name already exists, do not override it.

## 5. Testing

- Run the full suite with `mvn verify`. Do not use `--offline` unless dependencies are already cached.
- Tests live under `src/test/java/...` mirroring the main package layout. Unit tests end in `Tests`;
  Spring‑context / sample‑app integration tests live under `sampleapps/` and `integration/` and use the
  `spring-cloud-stream-test-binder`.
- Use JUnit 5 + AssertJ/Mockito; use Awaitility (already a dependency) for async assertions rather than
  `Thread.sleep`. Use Reactor Test (`StepVerifier`) for `Flux`/`Mono` behaviour.
- Any change to header parsing, deduplication (`ResponseHandler`), grouping (`MessageChunker`), or the
  wrap helpers must come with tests — these are the correctness‑critical parts.
- The starter must remain loadable **and** excludable: keep the tests that verify
  `@ImportAutoConfiguration(exclude = RequestReplyAutoConfiguration.class)` and
  `spring.autoconfigure.exclude` behaviour green.
- Do not require a live Solace broker for `src/test`; use the test binder. (The `examples/` apps may point
  at a public broker, but they are not part of the unit test suite.)

## 6. Project-specific patterns & invariants

- **Solace is optional.** Guard Solace‑specific beans/parsers with `@ConditionalOnClass` (as
  `RequestReplyAutoConfiguration` does for `SolaceHeaderParser`). Never make core request/reply logic hard‑
  depend on `sol-jcsmp`.
- **Reply consumers are contributed by an `ImportBeanDefinitionRegistrar`, not an
  `ApplicationContextInitializer`.** Spring Boot applies initializers to *every* context (including sliced
  test contexts) and they cannot be excluded, which previously broke `@JsonTest`/`@WebMvcTest` slices. Keep
  this registration inside `RequestReplyFunctionRegistrar` imported by the auto‑configuration so that
  excluding the auto‑configuration also disables the consumers.
- **Header access goes through the ordered parser chain**, never by reading a hard‑coded header key inline.
  To support a new binder or header convention, add a parser bean and order it with `@Order` (lower =
  higher priority). Header name constants live in `SpringHeaderParser` (`totalReplies`, `replyIndex`,
  `groupedMessages`, `groupedContentType`, `errorMessage`) — reuse them.
- **Correlation ids** are generated as a random UUID unless the request already carries one.
- **Reply‑to uniqueness:** reply topics should be process‑unique. Use `${replyTopicWithWildcards|uuid}`
  (generated once at process start) — not `${random.uuid}` (new value per reference). The requester listens
  on the wildcarded topic via `${replyTopicWithWildcards|<binding>|<wildcard>}`.
- **Deduplication** (`ResponseHandler`) is by `replyIndex`, backed by a `BitSet`; terminal (finish/error)
  messages bypass dedup. The unknown/streaming growth bound is read as a **JVM system property**
  `spring.cloud.stream.requestreply.dedup.maxBitsWhenUnknown` (via `Integer.getInteger`, default 100000) —
  it is **not** a Spring `@ConfigurationProperties` value, so it is set with `-D`, not `application.yaml`.
- **Grouping thresholds** are load‑bearing constants: flush at 1 MB, at 10 000 messages, or after the group
  timeout (default 200 ms). Changing them affects broker behaviour and interop — do so deliberately and
  document it.
- **Threading & context:** requests run on a shared executor wrapped with
  `ContextExecutorService`/`ContextSnapshotFactory` so Micrometer context (trace id, MDC) propagates across
  every pipeline stage. Preserve the executor‑wrapping approach; do not replace it with per‑task snapshots.
- **State is in memory** (`PENDING_RESPONSES`), so the library is not fail‑safe or horizontally scalable
  for a single request. Do not silently introduce assumptions that a reply may be handled by a different
  instance than the one that sent the request.
- **Configuration property namespace** is `spring.cloud.stream.requestreply`. Add new options to
  `RequestReplyProperties` / `BinderMappings` with getters/setters and document them in the README property
  table. Unknown YAML keys under a binding mapping are silently ignored by relaxed binding — do not rely on
  keys that have no field.

## 7. Example

A minimal requester + responder pair. Configuration ties the binding name across
`spring.cloud.function.definition`, `requestreply.bindingMapping`, and the `-in-0`/`-out-0` bindings.

```yaml
spring:
  cloud:
    function:
      definition: temperatureQuery
    stream:
      requestreply:
        bindingMapping:
          - binding: temperatureQuery
            replyTopic: reply/temperature/@project.artifactId@_${HOSTNAME}_${replyTopicWithWildcards|uuid}
            topicPatterns:
              - request/temperature/.*
      bindings:
        temperatureQuery-in-0:
          destination: ${replyTopicWithWildcards|temperatureQuery|*}
          contentType: "application/json"
          binder: solace
        temperatureQuery-out-0:
          binder: solace
```

```java
// Requester
SensorReading reading = requestReplyService.requestAndAwaitReplyToTopic(
        request,
        "request/temperature/celsius/livingroom",
        SensorReading.class,
        Duration.ofSeconds(30)
);

// Responder (Spring Cloud Function bean)
@Bean
public Function<Message<SensorRequest>, Message<SensorReading>> temperatureQuery(
        RequestReplyMessageHeaderSupportService headerSupport
) {
    return headerSupport.wrap(req -> {
        SensorReading r = new SensorReading();
        r.setTemperature(21.5);
        return r;                  // return null to drop; throw a forwarded exception to send an error reply
    }, MyBusinessException.class);
}
```

## Related projects

Only public ecosystem projects are relevant here — do not reference private/internal systems:

- [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) — the messaging abstraction this
  library builds on.
- [Spring Cloud Function](https://spring.io/projects/spring-cloud-function) — the functional model used for
  responders and reply consumers.
- [Spring Boot](https://spring.io/projects/spring-boot) — auto‑configuration and starter mechanics.
- [Solace Spring Cloud / PubSub+ binder](https://github.com/SolaceProducts/solace-spring-cloud) — the
  primary target binder.
- [Project Reactor](https://projectreactor.io/) — reactive types for multi‑reply.
- [Micrometer Context Propagation](https://docs.micrometer.io/context-propagation/reference/) — tracing/MDC
  propagation across threads.
