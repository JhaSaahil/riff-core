package com.riff.core.api;

import com.riff.core.api.dto.ClipResponse;
import com.riff.core.api.dto.CreateClipRequest;
import com.riff.core.service.ClipService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON API for creating, resolving, and deleting clips.
 *
 * <ul>
 *   <li>{@code POST /}        — authenticated; snapshots track metadata, returns 201.</li>
 *   <li>{@code GET /{slug}}   — anonymous; pure cache/DB read on the hot path.</li>
 *   <li>{@code DELETE /{slug}}— authenticated, owner only; soft delete.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clips")
public class ClipController {

    private final ClipService clipService;

    public ClipController(ClipService clipService) {
        this.clipService = clipService;
    }

    /**
     * Create a clip. The service performs the Spotify snapshot, range validation
     * against the real track duration, and slug-collision retry.
     *
     * @return 201 Created with a {@code Location: /c/{slug}} header
     */
    @PostMapping
    public ResponseEntity<ClipResponse> create(@Valid @RequestBody CreateClipRequest request) {
        UUID userId = currentUserId();
        ClipResponse response = clipService.create(request, userId);
        return ResponseEntity
                .created(URI.create("/c/" + response.slug()))
                .body(response);
    }

    /**
     * Resolve a clip as JSON for the SPA. No authentication — unguessable slugs
     * stand in for access control on the read path.
     */
    @GetMapping("/{slug}")
    public ClipResponse resolve(@PathVariable String slug) {
        return clipService.resolve(slug);
    }

    /**
     * Soft-delete a clip. Ownership is enforced in the service; a non-owner gets
     * a 403 via the global exception handler.
     */
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        UUID userId = currentUserId();
        clipService.delete(slug, userId);
        return ResponseEntity.noContent().build();
    }

    private static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }
}
