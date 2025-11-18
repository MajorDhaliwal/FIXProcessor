package com.example.fixproducer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads FIX log lines from a file and pushes them to a Redis Stream.
 *
 * Usage (locally):
 *   mvn exec:java -Dexec.mainClass="com.example.fixproducer.FixProducer" \
 *       -Dexec.args="/path/to/fix.log"
 *
 * Usage (inside Docker):
 *   Set REDIS_HOST=redis and REDIS_PORT=6379 via environment variables.
 */
public class FixProducer {

    private static final String STREAM_NAME = "fix-messages";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: mvn exec:java -Dexec.mainClass=\"com.example.fixproducer.FixProducer\" -Dexec.args=\"/path/to/fix.log\"");
            System.exit(1);
        }

        String filePath = args[0];

        // Allow running both locally and inside Docker
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

        System.out.println("Reading FIX messages from: " + filePath);
        System.out.println("Connecting to Redis at " + redisHost + ":" + redisPort);

        try (Jedis jedis = new Jedis(redisHost, redisPort);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                // Split by the standard FIX header pattern
                String[] messages = line.split("8=FIX\\.4\\.4");
                for (String msg : messages) {
                    if (msg.isBlank()) continue;

                    String fixMessage = "8=FIX.4.4" + msg.trim();

                    Map<String, String> entry = new HashMap<>();
                    entry.put("fixMessage", fixMessage);

                    jedis.xadd(STREAM_NAME, StreamEntryID.NEW_ENTRY, entry);
                    count++;

                    if (count % 1000 == 0) {
                        System.out.println("Sent " + count + " messages so far...");
                    }
                }
            }

            System.out.println("Done! Total messages sent: " + count);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Redis error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

