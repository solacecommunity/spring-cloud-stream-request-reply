/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.controller;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;

@RestController
public class RequestReplyController {
    private RequestReplyService requestReplyService;
    private StreamBridge streamBridge;

    @Autowired
    public RequestReplyController(RequestReplyService requestReplyService, StreamBridge streamBridge) {
        this.requestReplyService = requestReplyService;
        this.streamBridge=streamBridge;
    }

    @PostMapping("/request/{requestTo}/reply/{replyTo}")
    public void requestReplyAsync( //
            @RequestHeader HttpHeaders headers, //
            @RequestBody byte[] data, //
            @PathVariable(value = "requestTo") @NotEmpty String requestTo, //
            @PathVariable(value = "replyTo") @NotEmpty String replyTo //
    ) {
        requestReplyService.requestReply(data, requestTo, replyTo);
    }

    @PostMapping("/reverseText")
    public String requestReplySync( //
            @RequestHeader(required = false) HttpHeaders headers, //
            @RequestBody String data //
    ) throws InterruptedException, ExecutionException {
        Future<String> response = requestReplyService.requestAndAwaitReply(data, "requestReply-mainSession-requests-out-0", String.class);
        return response.get();
    }
}
