/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.controller;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
public class RequestReplyController {
    private RequestReplyService requestReplyService;

    @Autowired
    public RequestReplyController(RequestReplyService requestReplyService) {
        this.requestReplyService = requestReplyService;
    }

    @PostMapping("/request/{requestTo}/reply/{replyTo}")
    public void requestReplyAsync( //
            @RequestHeader HttpHeaders headers, //
            @RequestBody byte[] data, //
            @PathVariable(value = "requestTo") @NotEmpty String requestTo, //
            @PathVariable(value = "replyTo") @NotEmpty String replyTo //
    ) {
        requestReplyService.requestReply(data, requestTo, replyTo, 2000);
    }

    @PostMapping("/reverseText")
    public String requestReplySync( //
            @RequestHeader(required = false) HttpHeaders headers, //
            @RequestBody String data //
    ) throws InterruptedException, ExecutionException {
        Future<String> response = requestReplyService.requestAndAwaitReply(data, "reverse-in-0", 2000, String.class);
        return response.get();
    }
}
