package com.devicehive.security.util;

import com.devicehive.security.jwt.JwtPrincipal;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Convenience class to generate a token.
 */
@Component
public class JwtTokenGenerator {

    @Autowired
    protected Gson gson;

    @Value("${jwt.secret}")
    String secret;

    /**
     * Generates a JWT token containing all needed claims. These properties are taken from the specified
     * JwtPrincipal object.
     *
     * @param principal the principal entity for which the token will be generated
     * @return the JWT token
     */
    public String generateToken(JwtPrincipal principal) {
        String val = gson.toJson(principal);
        Claims claims = Jwts.claims().setSubject(val);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
