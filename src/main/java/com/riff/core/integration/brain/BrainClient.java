package com.riff.core.integration.brain;

import com.riff.core.api.dto.TrackSummary;
import com.riff.core.domain.TimeRange;

import java.util.List;
import java.util.Optional;

/**
 * Seam over the {@code riff-brain} semantic service. In Phase 1 the only
 * implementation is {@link NoOpBrainClient}, which degrades semantic features to
 * empty results so callers never need to know brain is absent. Phase 2 supplies
 * {@link HttpBrainClient} with a circuit breaker and a keyword-search fallback.
 */
public interface BrainClient {

    /**
     * Semantic track search. Returns an empty list when brain is unavailable —
     * callers should fall back to keyword search rather than fail the request.
     */
    List<TrackSummary> semanticSearch(String query, int limit);

    /**
     * Suggests a clip time range for a track given a natural-language query
     * (e.g. "the drop", "the chorus"). Empty when unavailable or unsupported.
     */
    Optional<TimeRange> suggestRange(String trackId, String query);
}
