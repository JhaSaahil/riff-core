package com.riff.core.api;

import com.riff.core.api.dto.ClipResponse;
import com.riff.core.service.ClipService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated creator's own clips. Backed by the
 * {@code idx_clips_creator (created_by, created_at DESC)} index, so listing is a
 * cheap ordered range scan.
 */
@RestController
@RequestMapping("/api/v1/me")
@Validated
public class MeController {

    private final ClipService clipService;

    public MeController(ClipService clipService) {
        this.clipService = clipService;
    }

    /**
     * List the current user's clips, newest first.
     *
     * @param page zero-based page index
     * @param size page size (1–100, default 20)
     */
    @GetMapping("/clips")
    public Page<ClipResponse> myClips(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        UUID userId = currentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clipService.listByUser(userId, pageable);
    }

    private static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }
}
