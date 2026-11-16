package org.narxoz.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
    private static final String ISSUER = "purchase-requests";

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final TokenService tokens = new TokenService(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(15)));

    private NimbusJwtDecoder decoder(String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Test
    void issuedTokenCanBeReadBack() {
        TokenService.IssuedToken issued = tokens.issue("manager", List.of("ROLE_MANAGER"));

        Jwt jwt = decoder(ISSUER).decode(issued.value());

        assertEquals("manager", jwt.getSubject());
        assertEquals(List.of("MANAGER"), jwt.getClaimAsStringList("roles"));
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertEquals(900L, issued.expiresInSeconds());
    }

    @Test
    void tokenFromAnotherProductIsRejected() {
        TokenService.IssuedToken issued = tokens.issue("manager", List.of("ROLE_MANAGER"));

        assertThrows(JwtException.class, () -> decoder("helpdesk").decode(issued.value()));
    }

    @Test
    void shortSecretFailsAtStartup() {
        JwtProperties shortSecret = new JwtProperties("secret", ISSUER, Duration.ofMinutes(15));

        assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtKey(shortSecret));
    }
}
