package com.bidhub.account.service;

import com.bidhub.account.config.JwtProperties;
import com.bidhub.account.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getUserId().toString())
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", user.getRole().name()))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return properties.getExpirationMs();
    }
}
