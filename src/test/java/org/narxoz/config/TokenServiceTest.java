package org.narxoz.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
    private static final String ISSUER = "purchase-requests";

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties props = new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(15));
    private final SecretKey key = config.jwtKey(props);
    private final TokenService tokens = new TokenService(config.jwtEncoder(key), props);

    @Test
    void issuedTokenCanBeReadBack() {
        TokenService.IssuedToken issued = tokens.issue("manager", List.of("ROLE_MANAGER"));

        Jwt jwt = config.jwtDecoder(key, props).decode(issued.value());

        assertEquals("manager", jwt.getSubject());
        assertEquals(List.of("MANAGER"), jwt.getClaimAsStringList("roles"));
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        Instant expiresAt = jwt.getExpiresAt();
        assertNotNull(expiresAt, "JWT must contain an expiration time");
        assertTrue(expiresAt.isAfter(Instant.now()));
        assertEquals(900L, issued.expiresInSeconds());
    }

    @Test
    void tokenFromAnotherProductIsRejected() {
        JwtProperties otherProduct = new JwtProperties(SECRET, "helpdesk", props.ttl());
        var issued = new TokenService(config.jwtEncoder(key), otherProduct)
                .issue("manager", List.of("ROLE_MANAGER"));

        assertThrows(JwtException.class, () -> config.jwtDecoder(key, props).decode(issued.value()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void expiredOrMissingExpiryIsRejected(boolean includeExpiration) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).subject("manager").issuedAt(now.minusSeconds(901));
        if (includeExpiration) {
            claims.expiresAt(now.minusSeconds(1));
        }
        String value = config.jwtEncoder(key)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();

        assertThrows(JwtException.class, () -> config.jwtDecoder(key, props).decode(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "secret")
    void missingOrShortSecretIsRejected(String secret) {
        JwtProperties shortSecret = new JwtProperties(secret, ISSUER, Duration.ofMinutes(15));

        assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtKey(shortSecret));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "я", "😀"})
    void secretRequiresAtLeast32UnicodeCodePoints(String character) {
        JwtConfig config = new JwtConfig();
        Duration ttl = Duration.ofMinutes(15);
        JwtProperties tooShort = new JwtProperties(character.repeat(31), ISSUER, ttl);
        assertThrows(IllegalStateException.class, () -> config.jwtKey(tooShort));

        JwtProperties minimum = new JwtProperties(character.repeat(32), ISSUER, ttl);
        SecretKey jwtKey = config.jwtKey(minimum);
        var issuer = new TokenService(config.jwtEncoder(jwtKey), minimum);
        var issued = issuer.issue("manager", List.of("ROLE_MANAGER"));
        assertEquals("manager", config.jwtDecoder(jwtKey, minimum).decode(issued.value()).getSubject());
    }
}
