package com.devicehive.security.util;

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to generate a token.
 */
@Component
public class JwtTokenGenerator {

    @Value("${jwt.secret}")
    String secret;

    @Value("${jwt.refresh-token-max-age}")
    long refreshTokenMaxAge;

    @Value("${jwt.access-token-max-age}")
    long accessTokenMaxAge;

    /**
     * Generates a JWT token containing all needed claims. These properties are taken from the specified
     * JwtPayload object.
     *
     * @param payload the payload entity with which the token will be generated
     * @return the JWT token
     */
    public String generateToken(JwtPayload payload, TokenType tokenType) {
        
        long maxAge = tokenType.equals(TokenType.ACCESS) ? accessTokenMaxAge : refreshTokenMaxAge;
        Date expiration = new Date(System.currentTimeMillis() + maxAge);
        
        payload.setExpiration(expiration);
        payload.setTokenType(tokenType);
        
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtPayload.JWT_CLAIM_KEY, payload);

        Claims claims = Jwts.claims(jwtMap);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
}
