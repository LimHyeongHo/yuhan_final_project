package com.Nbbang.backend.global.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Slf4j
@Component
@Profile("prod")
public class KeepAliveScheduler {

    private static final String LIVENESS_PATH = "/actuator/health/liveness";

    private final boolean enabled;
    private final Instant keepAliveUntil;
    private final URI pingUri;
    private final HttpClient httpClient;

    public KeepAliveScheduler(
            @Value("${app.keep-alive.enabled:false}") boolean enabled,
            @Value("${app.keep-alive.until:}") String keepAliveUntil,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.enabled = enabled;
        this.keepAliveUntil = keepAliveUntil.isBlank()
                ? null
                : OffsetDateTime.parse(keepAliveUntil).toInstant();
        this.pingUri = URI.create(removeTrailingSlash(publicBaseUrl) + LIVENESS_PATH);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Scheduled(
            initialDelay = 60_000,
            fixedDelayString = "${app.keep-alive.interval-ms:600000}"
    )
    public void keepAlive() {
        if (!shouldPing(Instant.now())) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(pingUri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        /// self ping 성공 시 render에 로그 기록
        try {
            int statusCode = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                log.warn("Render keep-alive ping failed: status={}", statusCode);
            } else {
                log.info("Render keep-alive ping succeeded: status={}, uri={}, at={}",
                        statusCode, pingUri, Instant.now());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Render keep-alive ping interrupted");
        } catch (IOException exception) {
            log.warn("Render keep-alive ping failed: errorType={}", exception.getClass().getSimpleName());
        }
    }

    boolean shouldPing(Instant now) {
        return enabled && (keepAliveUntil == null || now.isBefore(keepAliveUntil));
    }

    private static String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
