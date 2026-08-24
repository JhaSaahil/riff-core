package com.riff.core.api;

import com.riff.core.service.AuthService;
import com.riff.core.service.AuthService.LoginChallenge;
import com.riff.core.service.AuthService.SessionToken;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spotify OAuth (Authorization Code + PKCE) for creators. The service exchanges
 * the code with Spotify, upserts the local user by Spotify user id, and issues
 * this service's own JWT — Spotify tokens are never proxied to the client.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Begin login: build the Spotify authorize URL (with state) and redirect.
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        LoginChallenge challenge = authService.beginLogin();
        return ResponseEntity
                .status(302)
                .location(URI.create(challenge.authorizeUrl()))
                .build();
    }

    /**
     * Spotify redirects back here with an authorization code and state.
     * Exchange, find-or-create the user, and return a JWT.
     */
    @GetMapping("/callback")
    public SessionToken callback(@RequestParam("code") String code,
                                 @RequestParam("state") String state) {
        return authService.completeLogin(code, state);
    }

    /**
     * Return the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public Map<String, Object> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) auth.getPrincipal();
        return authService.currentUser(userId);
    }
}
