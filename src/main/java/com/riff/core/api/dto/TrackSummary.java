package com.riff.core.api.dto;

/**
 * Compact track representation returned by track search (keyword and, later,
 * semantic). This is what the trim UI renders as search results before a clip
 * exists.
 */
public record TrackSummary(
        String id,
        String title,
        String artist,
        String artworkUrl,
        Integer durationMs
) {
}
