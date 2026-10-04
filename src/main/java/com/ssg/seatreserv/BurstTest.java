package com.ssg.seatreserv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual concurrency/burst test for the seat reservation API.
 *
 * Run the Spring Boot application first, then run this main class.
 * Optional args:
 *   args[0] = base URL       (default http://localhost:8080)
 *   args[1] = request count  (default 500)
 *
 * The test:
 *  1. Creates a user and obtains its token.
 *  2. Verifies POST /createShow is public.
 *  3. Verifies GET /show/{id} is public.
 *  4. Sends many authenticated requests for the same seat at once.
 *  5. Asserts exactly one reservation succeeds.
 */
public class BurstTest {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_TOTAL_REQUESTS = 500;
    private static final String TARGET_SEAT = "A1";

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : DEFAULT_BASE_URL;
        int totalRequests = args.length > 1
                ? Integer.parseInt(args[1])
                : DEFAULT_TOTAL_REQUESTS;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            String token = createUser(client, baseUrl);
            String showId = createShowWithoutAuthentication(client, baseUrl);
            verifyShowStateIsPublic(client, baseUrl, showId);

            runReservationBurst(client, baseUrl, token, showId, totalRequests);
        }
    }

    private static String createUser(HttpClient client, String baseUrl) throws Exception {
        long mobile = 9_000_000_000L + (System.nanoTime() % 999_999_999L);

        String requestBody = """
                {
                  "name": "Burst User",
                  "mobNo": %d
                }
                """.formatted(mobile);

        HttpResponse<String> response = sendJson(
                client,
                baseUrl + "/user/createUser",
                requestBody,
                null
        );

        require2xx("create user", response);

        String token = jsonString(response.body(), "token");
        if (token == null || token.isBlank()) {
            throw new AssertionError("Create-user response did not contain token. Body: " + response.body());
        }

        System.out.println("Created burst-test user. HTTP " + response.statusCode());
        return token;
    }

    /**
     * Intentionally sends no Authorization header.
     * This catches SecurityConfig regressions where /createShow is not permitAll().
     */
    private static String createShowWithoutAuthentication(HttpClient client, String baseUrl) throws Exception {
        String uniqueName = "BURST-" + UUID.randomUUID().toString().substring(0, 8);

        String requestBody = """
                {
                  "name": "%s",
                  "seats": ["A1", "A2", "A3", "A4", "A5"],
                  "pricePaise": 25000,
                  "perUserLimit": 4
                }
                """.formatted(uniqueName);

        HttpResponse<String> response = sendJson(
                client,
                baseUrl + "/createShow",
                requestBody,
                null
        );

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AssertionError(
                    "POST /createShow is expected to be public, but security returned HTTP "
                            + response.statusCode()
                            + ". Add /createShow to permitAll() in SecurityConfig. Body: "
                            + response.body()
            );
        }

        require2xx("create show", response);

        String showId = jsonString(response.body(), "showId");
        if (showId == null || showId.isBlank()) {
            throw new AssertionError("Create-show response did not contain showId. Body: " + response.body());
        }

        System.out.println("Created show: " + showId);
        return showId;
    }

    /**
     * Intentionally sends no Authorization header.
     */
    private static void verifyShowStateIsPublic(
            HttpClient client,
            String baseUrl,
            String showId
    ) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/show/" + showId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AssertionError(
                    "GET /show/{id} is expected to be public, but security returned HTTP "
                            + response.statusCode()
                            + ". Add GET /show/** to permitAll() in SecurityConfig."
            );
        }

        require2xx("show state", response);
        System.out.println("Public show-state check passed. HTTP " + response.statusCode());
    }

    private static void runReservationBurst(
            HttpClient client,
            String baseUrl,
            String token,
            String showId,
            int totalRequests
    ) throws Exception {

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalRequests);

        Map<Integer, AtomicInteger> statusCounts = new ConcurrentHashMap<>();
        AtomicInteger clientErrors = new AtomicInteger();

        System.out.printf(
                "Firing %,d concurrent reservation attempts at show=%s seat=%s%n",
                totalRequests,
                showId,
                TARGET_SEAT
        );

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();

                        String idempotencyKey = UUID.randomUUID().toString();
                        String body = """
                                {
                                  "seats": ["%s"],
                                  "idempotencyKey": "%s",
                                  "showId": "%s"
                                }
                                """.formatted(TARGET_SEAT, idempotencyKey, showId);

                        HttpResponse<String> response = sendJson(
                                client,
                                baseUrl + "/reserveSeats",
                                body,
                                token
                        );

                        statusCounts
                                .computeIfAbsent(response.statusCode(), ignored -> new AtomicInteger())
                                .incrementAndGet();

                    } catch (Exception e) {
                        clientErrors.incrementAndGet();
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            long start = System.nanoTime();
            startGate.countDown();
            endGate.await();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.println("\n--- Burst results ---");
            statusCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> System.out.printf(
                            "HTTP %d : %,d%n",
                            entry.getKey(),
                            entry.getValue().get()
                    ));
            System.out.println("Client-side failures: " + clientErrors.get());
            System.out.println("Elapsed: " + elapsedMs + " ms");

            int successes = statusCounts.getOrDefault(200, new AtomicInteger()).get();

            if (successes != 1) {
                throw new AssertionError(
                        "Concurrency invariant failed: expected exactly 1 successful reservation, got "
                                + successes
                                + ". Statuses=" + statusCounts
                                + ", clientErrors=" + clientErrors.get()
                );
            }

            if (clientErrors.get() != 0) {
                throw new AssertionError(
                        "HTTP client failed for " + clientErrors.get()
                                + " requests. Reduce burst size before treating this as an application failure."
                );
            }

            System.out.println("PASS: exactly one request reserved seat " + TARGET_SEAT + ".");
        }
    }

    private static HttpResponse<String> sendJson(
            HttpClient client,
            String url,
            String body,
            String token
    ) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (token != null && !token.isBlank()) {
            // Current application expects the raw token in Authorization.
            builder.header("Authorization", token);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void require2xx(String operation, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AssertionError(
                    operation + " failed with HTTP " + response.statusCode()
                            + ". Body: " + response.body()
            );
        }
    }

    private static String jsonString(String json, String field) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\""
        );
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
