package com.campushub.support.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.xml.bind.DatatypeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class JwtService {

    // IMPORTANT: This secret key should match the one used in the user-service for signing tokens
    @Value("${jwt.secret:defaultSecretKeyForTestingOnlyUseAStrongSecretInProduction}")
    private String secret;

    private Key getSigningKey() {
        byte[] secretBytes = DatatypeConverter.parseHexBinary(secret);
        return Keys.hmacShaKeyFor(secretBytes);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            // Log exception (e.g., SignatureException, ExpiredJwtException, MalformedJwtException)
            return false;
        }
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public List<GrantedAuthority> getAuthoritiesFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        // Assuming roles are stored as a comma-separated string or a list in the "role" claim
        // This needs to match how roles are added in the user-service's JwtService
        Object roleClaim = claims.get("role");
        if (roleClaim instanceof String) {
            return Stream.of(((String) roleClaim).split(","))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        }
        // Fallback or handle other types if necessary
        return Collections.emptyList();
    }

    public Long getIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("id", Long.class); // Assuming the ID is stored as a Long
    }
}
