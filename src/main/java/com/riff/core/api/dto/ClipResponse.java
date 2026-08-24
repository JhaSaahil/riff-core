package com.riff.core.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.riff.core.domain.Clip;
import java.time.Instant;

/**
 * JSON representation of a resolved clip, returned by the clip and preview APIs.
 *
 * <p>All track fields come from the snapshot stored on the {@link Clip} row at
 * creation time, so building this response never touches Spotify.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClipResponse(
        String slug,
        String url,
        TrackInfo track,
        Integer startMs,
        Integer endMs,
        Instant createdAt
) {

    /** Snapshotted track metadata carried on the clip. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackInfo(
            String id,
            String title,
            String artist,
            String artworkUrl
    ) {
    }

    /**
     * Build a response from a persisted clip using a default base URL.
     */
    public static ClipResponse from(Clip clip) {
        return fromEntity(clip, "https://riff.app");
    }

    /**
     * Build a response from a persisted clip.
     *
     * @param clip    the persisted clip entity (with its metadata snapshot)
     * @param baseUrl public base URL, e.g. {@code https://riff.app}; the share
     *                URL is {@code {baseUrl}/c/{slug}}
     */
    public static ClipResponse fromEntity(Clip clip, String baseUrl) {
        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        TrackInfo track = new TrackInfo(
                clip.getProviderTrackId(),
                clip.getTrackTitle(),
                clip.getTrackArtist(),
                clip.getArtworkUrl()
        );

        return new ClipResponse(
                clip.getSlug(),
                normalizedBase + "/c/" + clip.getSlug(),
                track,
                clip.getStartMs(),
                clip.getEndMs(),
                clip.getCreatedAt()
        );
    }
}
