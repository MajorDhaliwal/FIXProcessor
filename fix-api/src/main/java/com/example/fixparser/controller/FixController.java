package com.example.fixparser.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fix")
@Tag(name = "Fix Parsing API", description = "Endpoints for decoding and queuing FIX messages")
public class FixController {

    private final Jedis jedis;

    @Autowired
    public FixController(Jedis jedis) {
        this.jedis = jedis;
    }

    private static final Map<String, String> TAG_NAMES = Map.ofEntries(
        Map.entry("8", "BeginString"),
        Map.entry("9", "BodyLength"),
        Map.entry("35", "MsgType"),
        Map.entry("34", "MsgSeqNum"),
        Map.entry("49", "SenderCompID"),
        Map.entry("56", "TargetCompID"),
        Map.entry("52", "SendingTime"),
        Map.entry("11", "ClOrdID"),
        Map.entry("21", "HandlInst"),
        Map.entry("55", "Symbol"),
        Map.entry("54", "Side"),
        Map.entry("38", "OrderQty"),
        Map.entry("40", "OrdType"),
        Map.entry("44", "Price"),
        Map.entry("59", "TimeInForce"),
        Map.entry("10", "CheckSum")
    );

    @PostMapping("/parse")
    @Operation(
        summary = "Parse and queue FIX message",
        description = "Decodes a FIX message and pushes it to the Redis Stream for the FIX consumer to process."
    )
    public Map<String, Object> parseAndQueueFixMessage(

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Raw FIX message using | or SOH as delimiter",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "string",
                    example = "8=FIX.4.4|35=D|55=AAPL|54=1|38=100|44=150.25|10=999"
                )
            )
        )
        @RequestBody String fixMessage
    ) {

        if (fixMessage == null || fixMessage.isBlank()) {
            return Map.of("error", "FIX message cannot be empty");
        }

        // Remove quotes from Swagger JSON input
        fixMessage = fixMessage.trim();
        if (fixMessage.startsWith("\"") && fixMessage.endsWith("\"")) {
            fixMessage = fixMessage.substring(1, fixMessage.length() - 1);
        }

        // Normalize SOH delimiter
        fixMessage = fixMessage.replace("\u0001", "|");

        // Push into Redis Stream
        StreamEntryID redisId = jedis.xadd(
            "fix-messages",
            XAddParams.xAddParams(),
            Map.of("fixMessage", fixMessage)
        );

        System.out.println("Sent to Redis stream with ID: " + redisId);

        // Parse into key-value map
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String field : fixMessage.split("\\|")) {
            if (field.contains("=")) {
                String[] kv = field.split("=", 2);
                parsed.put(TAG_NAMES.getOrDefault(kv[0], "Unknown(" + kv[0] + ")"), kv[1]);
            }
        }

        return Map.of(
            "status", "queued",
            "redis_id", redisId.toString(),
            "parsed", parsed
        );
    }
}
