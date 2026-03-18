package Tutorial7_8.Store.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long expiresDays;

    public static final String COOKIE_NAME = "token";

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expires-days}") long expiresDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.expiresDays = expiresDays;
    }

    public String createToken(String userId, String email, String username,String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiresDays, ChronoUnit.DAYS)))
                .claim("email", email)
                .claim("username", username)
                .claim("type", type)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser().requireIssuer(issuer).verifyWith(key).build().parseSignedClaims(token);
    }

    /** put the jwt on the httponly cookie */
    public ResponseCookie buildCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)         // do not allow script to read the cookie
                .secure(false)          // http not https
                .path("/")
                .sameSite("Lax")
                .maxAge(expiresDays * 24 * 3600)
                .build();
    }
}
