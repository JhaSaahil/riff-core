package com.riff.core.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration placeholder.
 *
 * <p>For Phase 1, circuit breakers, retries, and timeouts for {@code SpotifyClient} and
 * {@code BrainClient} are declared entirely in {@code application.yml} under
 * {@code resilience4j.circuitbreaker.instances.*} and consumed via the
 * {@code @CircuitBreaker(name = "...")} annotation on the client methods. No programmatic
 * customizer beans are required yet.
 *
 * <p>Add a {@code Customizer<Resilience4JCircuitBreakerFactory>} bean here if we later need
 * per-instance config that cannot be expressed in YAML (e.g. dynamic thresholds).
 */
@Configuration
public class ResilienceConfig {
    // Intentionally empty for Phase 1 — configuration lives in application.yml.
}
