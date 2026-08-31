package com.riff.core.integration.brain;

import com.riff.core.api.dto.TrackSummary;
import com.riff.core.domain.TimeRange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * HTTP-backed brain client. Only wired when {@code riff.brain.enabled=true};
 * its presence disables {@link NoOpBrainClient} via that bean's
 * {@code @ConditionalOnMissingBean}.
 *
 * <p>Phase 2 implementation: this will call the {@code riff-brain} Python service
 * over HTTP, wrapped in a Resilience4j circuit breaker with a fallback to
 * {@code TrackSearchService.keywordSearch()}. For now it mirrors the no-op
 * behaviour so the seam is exercisable end to end without brain running.
 */
@Component
@ConditionalOnProperty(name = "riff.brain.enabled", havingValue = "true")
public class HttpBrainClient implements BrainClient {

    // Phase 2 implementation: inject WebClient/RestTemplate + Resilience4j
    // circuit breaker, call riff-brain, fall back to keyword search on failure.

    @Override
    public List<TrackSummary> semanticSearch(String query, int limit) {
        // Phase 2 implementation
        return List.of();
    }

    @Override
    public Optional<TimeRange> suggestRange(String trackId, String query) {
        // Phase 2 implementation
        return Optional.empty();
    }
}
