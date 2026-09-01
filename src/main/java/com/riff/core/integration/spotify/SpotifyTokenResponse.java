package com.riff.core.integration.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth token payload returned by the Spotify Accounts service
 * (https://accounts.spotify.com/api/token).
 *
 * <p>Note: {@code refresh_token} is only returned on the initial
 * authorization_code exchange; a refresh_token grant may omit it, in which
 * case the previously issued refresh token remains valid.
 */
public record SpotifyTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope) {
}
