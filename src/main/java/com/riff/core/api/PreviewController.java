package com.riff.core.api;

import com.riff.core.api.dto.ClipResponse;
import com.riff.core.service.ClipService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Server-rendered share preview. This is the hottest path and the one link
 * crawlers (WhatsApp, iMessage, Slack) fetch, so it must emit Open Graph tags in
 * static HTML — a React route would render as a bare URL because crawlers do not
 * run JavaScript.
 *
 * <p>Note: {@code @Controller}, not {@code @RestController} — the return value is
 * a Thymeleaf view name, not a serialized body.
 */
@Controller
public class PreviewController {

    private final ClipService clipService;
    private final String baseUrl;

    public PreviewController(ClipService clipService,
                             @Value("${riff.public-base-url:https://riff.app}") String baseUrl) {
        this.clipService = clipService;
        this.baseUrl = baseUrl;
    }

    /**
     * Resolve a clip and render the {@code preview} template with the model
     * attributes the template binds into {@code <meta property="og:*">} tags and
     * the progressive-enhancement deep link.
     *
     * @return the logical view name {@code "preview"}
     */
    @GetMapping("/c/{slug}")
    public String preview(@PathVariable String slug, Model model) {
        ClipResponse clip = clipService.resolve(slug);

        model.addAttribute("slug", clip.slug());
        model.addAttribute("title", clip.track().title());
        model.addAttribute("artist", clip.track().artist());
        model.addAttribute("artworkUrl", clip.track().artworkUrl());
        model.addAttribute("trackId", clip.track().id());
        model.addAttribute("startMs", clip.startMs());
        model.addAttribute("endMs", clip.endMs());
        model.addAttribute("shareUrl", baseUrl + "/c/" + clip.slug());

        // og:title convenience string, e.g. "Riff · 0:42–0:58"
        model.addAttribute("rangeLabel",
                "Riff \u00B7 " + formatMs(clip.startMs()) + "\u2013" + formatMs(clip.endMs()));

        return "preview";
    }

    /** Format a millisecond offset as {@code m:ss} for the OG title. */
    private static String formatMs(int ms) {
        Duration d = Duration.ofMillis(ms);
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + ":" + String.format("%02d", seconds);
    }
}
