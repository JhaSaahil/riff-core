package com.riff.core.api;

import com.riff.core.api.dto.TrackSummary;
import com.riff.core.service.TrackSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keyword track search, proxied and cached over Spotify. Authenticated because
 * it consumes the creator's rate budget and Spotify quota.
 */
@RestController
@RequestMapping("/api/v1/tracks")
@Validated
public class TrackSearchController {

    private final TrackSearchService trackSearchService;

    public TrackSearchController(TrackSearchService trackSearchService) {
        this.trackSearchService = trackSearchService;
    }

    /**
     * Search tracks by free-text query.
     *
     * @param query the search text
     * @param limit result cap (1–50, default 20)
     */
    @GetMapping("/search")
    public List<TrackSummary> search(
            @RequestParam("q") @NotBlank String query,
            @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(50) int limit) {
        return trackSearchService.search(query, limit);
    }
}
