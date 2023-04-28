/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.sampleapp.springbootstarter.requestreply.controller;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage.RemoteErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
                "abb1/abb2/abb3/d-echo/v1/demoApi/requestTopic",
                String.class,
                Duration.ofSeconds(2)
        );
    }
}
