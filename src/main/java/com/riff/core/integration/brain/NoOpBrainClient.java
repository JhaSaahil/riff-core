package com.riff.core.integration.brain;

import com.riff.core.api.dto.TrackSummary;
import com.riff.core.domain.TimeRange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Phase 1 default. Wired only when no {@link HttpBrainClient} bean is present,
 * so enabling brain in Phase 2 (via {@code riff.brain.enabled=true}) transparently
 * swaps the implementation with no change to callers.
 *
 * <p>All methods return empty results — the semantic features simply produce
 * nothing, and search controllers fall through to keyword search.
 */
@Component
@ConditionalOnMissingBean(HttpBrainClient.class)
public class NoOpBrainClient implements BrainClient {

    @Override
    public List<TrackSummary> semanticSearch(String query, int limit) {
        return List.of();
    }

    @Override
    public Optional<TimeRange> suggestRange(String trackId, String query) {
        return Optional.empty();
    }
}
