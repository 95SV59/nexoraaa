package com.nexora.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Pure logic tests — no Spring context, no real HTTP — matching the discipline used for
 *  the network pipeline's tests. The 32-byte-minimum secret requirement is exercised with
 *  a real (if throwaway) key; nothing here depends on application.yml. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-only-secret-at-least-32-bytes-long!!";
    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    @Test
    void tokenRoundTripsToTheSameUserIdAndUsername() {
        String token = jwtService.generateToken(42L, "ann");
        Optional<AuthPrincipal> principal = jwtService.validateAndGetPrincipal(token);
        assertTrue(principal.isPresent());
        assertEquals(42L, principal.get().userId());
        assertEquals("ann", principal.get().username());
        assertEquals("42", principal.get().getName(), "getName() must be the userId, not the username");
    }

    @Test
    void garbageTokenIsRejected() {
        assertTrue(jwtService.validateAndGetPrincipal("not-a-real-jwt").isEmpty());
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        String token = new JwtService("a-completely-different-32-byte-plus-secret!!", 60_000)
                .generateToken(1L, "bob");
        assertTrue(jwtService.validateAndGetPrincipal(token).isEmpty(),
                "a token from a different key must never validate — that's the whole point of signing");
    }

    /**
     * exp is a JWT "NumericDate" (RFC 7519 §4.1.4): whole seconds since epoch, no sub-second
     * precision. A 1ms expiration (tried first) almost always rounds to the *same* second as
     * issuance, so the token isn't reliably "expired" yet — not a JwtService bug, a property
     * of the spec. 2.5s comfortably clears one full second boundary either way.
     */
    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 500);
        String token = shortLived.generateToken(7L, "carl");
        Thread.sleep(2500);
        assertTrue(shortLived.validateAndGetPrincipal(token).isEmpty());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(5L, "dana");
        // Flip a character in the payload segment without re-signing — simulates a forged claim.
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[payload.length / 2] = payload[payload.length / 2] == 'a' ? 'b' : 'a';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];
        assertTrue(jwtService.validateAndGetPrincipal(tampered).isEmpty());
    }
}
