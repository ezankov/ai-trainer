package com.trainer.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility bean for generating and validating JWTs.
 *
 * <p>Tokens are signed with HMAC-SHA256. The secret and expiry are read from
 * {@code app.jwt.secret} and {@code app.jwt.expiration-ms} in {@code application.yml}.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT for the given username.
     *
     * <p>Claims: {@code sub=username}, {@code iat=now}, {@code exp=now+expirationMs}.
     *
     * @param username the subject to embed in the token
     * @return a compact, signed JWT string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the {@code sub} claim (username) from a JWT.
     *
     * @param token a compact JWT string
     * @return the username embedded in the token
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates a JWT against the given {@link UserDetails}.
     *
     * <p>A token is considered valid when:
     * <ul>
     *   <li>the signature is correct (verified during parsing),</li>
     *   <li>the token has not expired, and</li>
     *   <li>the {@code sub} claim matches {@code userDetails.getUsername()}.</li>
     * </ul>
     *
     * @param token       a compact JWT string
     * @param userDetails the user to validate against
     * @return {@code true} if the token is valid for the given user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            Date expiry = claims.getExpiration();
            return subject != null
                    && subject.equals(userDetails.getUsername())
                    && expiry != null
                    && expiry.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
