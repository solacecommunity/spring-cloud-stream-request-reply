/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2026.
 */

package community.solace.spring.cloud.requestreply.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTStream;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Small test helper to build {@link SDTStream} payloads compatible with the grouped-messages
 * parsing in {@link RequestReplyServiceImpl}.
 */
final class TestSdtStreamSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestSdtStreamSupport() {
    }

    static SDTStream createSdtStream(List<?> payloads) {
        SDTStream stream = JCSMPFactory.onlyInstance().createStream();
        for (Object payload : payloads) {
            stream.writeString("TextMessage");
            stream.writeBytes(serialize(payload));
        }
        return stream;
    }

    private static byte[] serialize(Object payload) {
        if (payload == null) {
            return new byte[0];
        }
        if (payload instanceof String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            // Fallback to toString; tests shouldn't fail due to serialization.
            return String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
        }
    }
}
