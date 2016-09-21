package com.devicehive.security.util;

import com.devicehive.security.jwt.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to generate a token.
 */
@Component
public class JwtTokenGenerator {

    @Value("${jwt.secret}")
    String secret;

    /**
     * Generates a JWT token containing all needed claims. These properties are taken from the specified
     * JwtPayload object.
     *
     * @param payload the payload entity with which the token will be generated
     * @return the JWT token
     */
    public String generateToken(JwtPayload payload) {
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtPayload.JWT_CLAIM_KEY, payload);

        Claims claims = Jwts.claims(jwtMap);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
