package com.example.fixparser;

import com.example.fixparser.model.FixMessageEntity;
import com.example.fixparser.repository.FixMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class FixStreamConsumer implements CommandLineRunner {

    @Autowired
    private FixMessageRepository repository;

    @Value("${spring.redis.host:redis}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private static final Pattern FIELD_PATTERN = Pattern.compile("\\|");
    private static final String STREAM_KEY = "fix-messages";
    private static final Path LAST_ID_FILE = Paths.get("last_stream_id.txt");

    @Override
    public void run(String... args) {
        System.out.println("Starting FixStreamConsumer (resuming from last ID if available)...");

        // Wait for Redis to finish initializing
        try {
            System.out.println("Waiting 5 seconds before connecting to Redis...");
            Thread.sleep(5_000);
        } catch (InterruptedException ignored) {
        }

        StreamEntryID lastId = loadLastStreamId();

        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            System.out.println("Connected to Redis at " + redisHost + ":" + redisPort);

            while (true) {
                XReadParams params = new XReadParams().count(1).block(2000);
                List<Map.Entry<String, List<StreamEntry>>> streams =
                        jedis.xread(params, Map.of(STREAM_KEY, lastId));

                if (streams != null && !streams.isEmpty()) {
                    for (Map.Entry<String, List<StreamEntry>> stream : streams) {
                        for (StreamEntry entry : stream.getValue()) {
                            lastId = entry.getID();
                            String fixMessage = entry.getFields().get("fixMessage");

                            if (fixMessage != null) {
                                Map<String, String> parsed = parseFixMessage(fixMessage);
                                saveToDatabase(parsed, fixMessage);
                                saveLastStreamId(lastId);
                            }
                        }
                    }
                } else {
                    System.out.println("Waiting for new messages...");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in FixStreamConsumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load the last processed stream ID from file (defaults to 0-0). */
    private StreamEntryID loadLastStreamId() {
        try {
            if (Files.exists(LAST_ID_FILE)) {
                String id = Files.readString(LAST_ID_FILE).trim();
                System.out.println("📖 Resuming from stream ID: " + id);
                return new StreamEntryID(id);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed to read last stream ID: " + e.getMessage());
        }
        System.out.println("🆕 No previous ID found, starting from 0-0.");
        return new StreamEntryID("0-0");
    }

    /** Persist the last processed stream ID to disk. */
    private void saveLastStreamId(StreamEntryID id) {
        try {
            Files.writeString(LAST_ID_FILE, id.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("⚠️ Could not save last stream ID: " + e.getMessage());
        }
    }

    private Map<String, String> parseFixMessage(String fixMessage) {
        fixMessage = fixMessage.replace("\u0001", "|");
        String[] fields = FIELD_PATTERN.split(fixMessage);
        Map<String, String> map = new HashMap<>();
        for (String field : fields) {
            if (field.contains("=")) {
                String[] kv = field.split("=", 2);
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private void saveToDatabase(Map<String, String> map, String raw) {
        FixMessageEntity entity = FixMessageEntity.builder()
                .msgType(map.get("35"))
                .symbol(map.get("55"))
                .side(map.get("54"))
                .orderQty(map.get("38"))
                .price(map.get("44"))
                .senderCompID(map.get("49"))
                .targetCompID(map.get("56"))
                .sendingTime(map.get("52"))
                .rawMessage(raw)
                .build();

        repository.save(entity);
        System.out.println("💾 Saved FIX message: " + entity.getSymbol() + " " + entity.getSide());
    }
}
