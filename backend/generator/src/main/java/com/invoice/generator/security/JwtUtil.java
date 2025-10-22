package com.invoice.generator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // IMPORTANT: See explanation on secret keys below
    private final String SECRET_KEY = "Aayushg2006@20/09_Aarna@2023@27/10";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Updated to use parserBuilder()
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        return createToken(userDetails.getUsername());
    }

// In JwtUtil.java

    private String createToken(String subject) {
        long now = System.currentTimeMillis();
        long expirationTime = 1000L * 60 * 60 * 24 * 30; // 30 Days in milliseconds

        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + expirationTime)) // Set expiration to 30 days from now
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Helper method to convert the String key to a Key object
    private Key getSigningKey() {
        byte[] keyBytes = this.SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}