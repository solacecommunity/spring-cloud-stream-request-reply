package community.solace.spring.cloud.requestreply.examples.response.config;

public record CustomHeaderConfig(
        /*
         * Defines which header within the solace messages should be used by the library
         * to identify the destination to send replies to.
         */
        String replyHeaderName
) {
}
