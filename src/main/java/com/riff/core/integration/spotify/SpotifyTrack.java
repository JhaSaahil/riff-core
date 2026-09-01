package com.riff.core.integration.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.riff.core.api.dto.TrackSummary;

/**
 * Subset of the Spotify track object returned by {@code GET /v1/tracks/{id}}
 * and embedded in search results.
 *
 * <p>Only the fields Riff snapshots onto a clip are mapped; unknown fields are
 * ignored so the mapping survives Spotify adding new attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyTrack(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("artists") SpotifyArtist[] artists,
        @JsonProperty("album") SpotifyAlbum album,
        @JsonProperty("duration_ms") Integer durationMs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyArtist(
            @JsonProperty("name") String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyAlbum(
            @JsonProperty("images") SpotifyImage[] images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyImage(
            @JsonProperty("url") String url,
            @JsonProperty("height") Integer height,
            @JsonProperty("width") Integer width) {
    }

    /**
     * Joins all artist names with ", ". Returns an empty string when the
     * artist array is absent or empty.
     */
    public String artistNames() {
        if (artists == null || artists.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < artists.length; i++) {
            if (artists[i] == null || artists[i].name() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(artists[i].name());
        }
        return sb.toString();
    }

    /**
     * Returns the first (typically largest) album image URL, or {@code null}
     * when no artwork is available.
     */
    public String artworkUrl() {
        if (album == null || album.images() == null || album.images().length == 0) {
            return null;
        }
        SpotifyImage first = album.images()[0];
        return first == null ? null : first.url();
    }

    /**
     * Projects this track into the API-facing {@link TrackSummary} DTO. This is
     * the shape snapshotted onto a clip and returned from search.
     */
    public TrackSummary toSummary() {
        return new TrackSummary(id, name, artistNames(), artworkUrl(), durationMs);
    }
}
