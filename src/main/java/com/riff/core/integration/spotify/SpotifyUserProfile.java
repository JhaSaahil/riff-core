package com.riff.core.integration.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the Spotify user object returned by {@code GET /v1/me}.
 * Only the fields Riff needs to identify a creator are mapped.
 */
public record SpotifyUserProfile(
        @JsonProperty("id") String id,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("email") String email) {
}
