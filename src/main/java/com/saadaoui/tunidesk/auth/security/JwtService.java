package com.saadaoui.tunidesk.auth.security;

import com.saadaoui.tunidesk.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    void validateSecret() {

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "application.security.jwt.secret-key is not configured. Set JWT_SECRET "
                            + "to a Base64-encoded value of at least 32 bytes before starting.");
        }

        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "application.security.jwt.secret-key is not a valid Base64 string.", ex);
        }

        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "application.security.jwt.secret-key must decode to at least 32 bytes (256 bits) "
                            + "for HS256. Current key is only " + decoded.length + " bytes.");
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver) {

        Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", user.getRoles().stream().map(role -> role.getName()).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public List<String> extractRoles(String token) {

        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public boolean isTokenValid(String token,
                                User user) {

        final String username = extractUsername(token);

        return username.equals(user.getEmail())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    private Date extractExpiration(String token) {

        return extractClaim(token,
                Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    private SecretKey getSignInKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        return Keys.hmacShaKeyFor(keyBytes);

    }

}
