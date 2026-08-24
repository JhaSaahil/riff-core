package com.riff.core.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a clip.
 *
 * <p>Bean-validation layer: rejects garbage before it costs a Spotify call. The
 * service layer still enforces {@code endMs <= track.durationMs} (needs external
 * data) and the database enforces the same range constraints as a backstop.
 */
public record CreateClipRequest(

        @NotBlank
        String provider,

        @NotBlank
        String trackId,

        @NotNull
        @Min(0)
        Integer startMs,

        @NotNull
        @Min(1)
        Integer endMs
) {

    /** Maximum selectable clip length, mirrored by the DB {@code range_bounded} CHECK. */
    private static final int MAX_DURATION_MS = 30_000;

    /**
     * Compact constructor applies the {@code provider} default. A blank/null
     * provider falls back to {@code "spotify"}; {@code @NotBlank} therefore only
     * fires if a caller sends an explicitly blank string that we do not default.
     */
    public CreateClipRequest {
        if (provider == null || provider.isBlank()) {
            provider = "spotify";
        }
    }

    /**
     * Cross-field rule: the end of the range must come strictly after the start.
     * Guarded against nulls so the {@code @NotNull} messages surface on their own
     * fields rather than being masked by a false here.
     */
    @AssertTrue(message = "endMs must be greater than startMs")
    public boolean isRangeOrdered() {
        if (startMs == null || endMs == null) {
            return true;
        }
        return endMs > startMs;
    }

    /**
     * Cross-field rule: the selected range may not exceed {@value #MAX_DURATION_MS} ms.
     */
    @AssertTrue(message = "clip length must not exceed 30000ms")
    public boolean isRangeBounded() {
        if (startMs == null || endMs == null) {
            return true;
        }
        return (endMs - startMs) <= MAX_DURATION_MS;
    }
}
