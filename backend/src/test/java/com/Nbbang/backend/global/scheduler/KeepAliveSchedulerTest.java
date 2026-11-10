package com.Nbbang.backend.global.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class KeepAliveSchedulerTest {

    private static final String PUBLIC_URL = "https://example.onrender.com";

    @Test
    void pingsOnlyBeforeConfiguredEndTime() {
        KeepAliveScheduler scheduler = new KeepAliveScheduler(
                true,
                "2026-11-16T00:00:00+09:00",
                PUBLIC_URL
        );

        assertThat(scheduler.shouldPing(Instant.parse("2026-11-15T14:59:59Z"))).isTrue();
        assertThat(scheduler.shouldPing(Instant.parse("2026-11-15T15:00:00Z"))).isFalse();
    }

    @Test
    void doesNotPingWhenDisabled() {
        KeepAliveScheduler scheduler = new KeepAliveScheduler(false, "", PUBLIC_URL);

        assertThat(scheduler.shouldPing(Instant.parse("2026-11-15T00:00:00Z"))).isFalse();
    }
}
