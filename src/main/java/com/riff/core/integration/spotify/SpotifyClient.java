package com.riff.core.integration.spotify;

import com.riff.core.api.dto.TrackSummary;
import com.riff.core.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Thin client over the Spotify Web API and Accounts service.
 *
 * <p>Scope per the design: Spotify is only touched on the auth and
 * search/create paths — never on the hot resolve path. Failures are logged and
 * surfaced as {@link ApiException} so the global handler can render RFC 7807
 * problem responses; the blast radius stays contained to the calling feature.
 */
@Component
public class SpotifyClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyClient.class);

    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public SpotifyClient(
            RestTemplate restTemplate,
            @Value("${riff.spotify.client-id}") String clientId,
            @Value("${riff.spotify.client-secret}") String clientSecret,
            @Value("${riff.spotify.redirect-uri}") String redirectUri) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * Fetches a single track. Used on the clip-creation path to snapshot
     * metadata (title, artist, artwork, duration) onto the clip row.
     *
     * @throws ApiException 404 when the track does not exist, 502 for any other
     *                      upstream failure.
     */
    public SpotifyTrack getTrack(String trackId) {
        String url = API_BASE + "/tracks/" + trackId;
        try {
            ResponseEntity<SpotifyTrack> response =
                    restTemplate.exchange(url, HttpMethod.GET, bearerEntity(appToken()), SpotifyTrack.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Spotify track not found: {}", trackId);
            throw new ApiException(HttpStatus.NOT_FOUND, "Track not found: " + trackId);
        } catch (HttpStatusCodeException e) {
            log.error("Spotify getTrack failed for {}: status={} body={}",
                    trackId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify track lookup failed");
        } catch (RestClientException e) {
            log.error("Spotify getTrack transport error for {}", trackId, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify unreachable");
        }
    }

    /**
     * Full-text track search, proxied and (upstream) cached. Returns a bounded
     * list of lightweight summaries for the search UI.
     *
     * @throws ApiException 503 when Spotify search is unavailable — search
     *                      degrades but clip create/resolve are unaffected.
     */
    public List<TrackSummary> searchTracks(String query, int limit) {
        String url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", limit)
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        try {
            ResponseEntity<SpotifySearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, bearerEntity(appToken()), SpotifySearchResponse.class);
            SpotifySearchResponse body = response.getBody();
            if (body == null || body.tracks() == null || body.tracks().items() == null) {
                return List.of();
            }
            List<TrackSummary> summaries = new ArrayList<>();
            for (SpotifyTrack track : body.tracks().items()) {
                if (track != null) {
                    summaries.add(track.toSummary());
                }
            }
            return summaries;
        } catch (HttpStatusCodeException e) {
            log.error("Spotify search failed for query='{}': status={} body={}",
                    query, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Spotify search unavailable");
        } catch (RestClientException e) {
            log.error("Spotify search transport error for query='{}'", query, e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Spotify search unavailable");
        }
    }

    /**
     * Exchanges an authorization code (Authorization Code + PKCE) for tokens.
     */
    public SpotifyTokenResponse getAccessToken(String authCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authCode);
        form.add("redirect_uri", redirectUri);
        return requestToken(form, "authorization_code");
    }

    /**
     * Refreshes an access token using a previously issued refresh token.
     */
    public SpotifyTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return requestToken(form, "refresh_token");
    }

    /**
     * Reads the profile of the user owning {@code accessToken}. Used once during
     * OAuth to resolve the Spotify user id we persist against a creator.
     */
    public SpotifyUserProfile getCurrentUser(String accessToken) {
        String url = API_BASE + "/me";
        try {
            ResponseEntity<SpotifyUserProfile> response =
                    restTemplate.exchange(url, HttpMethod.GET, bearerEntity(accessToken), SpotifyUserProfile.class);
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Spotify /me rejected token as unauthorized");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Spotify session expired");
        } catch (HttpStatusCodeException e) {
            log.error("Spotify getCurrentUser failed: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify profile lookup failed");
        } catch (RestClientException e) {
            log.error("Spotify getCurrentUser transport error", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify unreachable");
        }
    }

    // --- internals -------------------------------------------------------

    private SpotifyTokenResponse requestToken(MultiValueMap<String, String> form, String grant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<SpotifyTokenResponse> response =
                    restTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity, SpotifyTokenResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("Spotify token grant '{}' rejected: status={} body={}",
                    grant, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Spotify authorization failed");
        } catch (HttpStatusCodeException e) {
            log.error("Spotify token grant '{}' failed: status={} body={}",
                    grant, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify token endpoint failed");
        } catch (RestClientException e) {
            log.error("Spotify token grant '{}' transport error", grant, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify unreachable");
        }
    }

    /**
     * Client-credentials token for app-only calls (track lookup, search).
     * Kept minimal here; a production build would cache this until expiry.
     */
    private String appToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        SpotifyTokenResponse token = requestToken(form, "client_credentials");
        return token == null ? null : token.accessToken();
    }

    private HttpEntity<Void> bearerEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private String basicAuthHeader() {
        String creds = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    /** Envelope for {@code GET /v1/search?type=track}. */
    private record SpotifySearchResponse(Tracks tracks) {
        private record Tracks(SpotifyTrack[] items) {
        }
    }
}
