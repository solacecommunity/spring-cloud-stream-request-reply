package community.solace.spring.cloud.requestreply.sampleapps.simple.controller;

import community.solace.spring.cloud.requestreply.service.RequestReplyService;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RestController
public class RequestReplyController {
    private final RequestReplyService requestReplyService;

    @Autowired
    public RequestReplyController(RequestReplyService requestReplyService) {
        this.requestReplyService = requestReplyService;
    }

    @PostMapping("/reverseText")
    public String requestReplySync(
            @RequestBody String data
    ) throws InterruptedException, TimeoutException, RemoteErrorException {
        return requestReplyService.requestAndAwaitReplyToTopic(
                data,
                "abb1/abb2/abb3/d-pampelmuse/v1/demoApi/requestTopic",
                String.class,
                Duration.ofSeconds(2)
        );
    }
}
