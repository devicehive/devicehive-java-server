package com.devicehive.service.security.jwt;

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.util.JwtTokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class responsible for access and refresh JWT keys generation.
 */
@Component
public class JwtClientService {

    @Value("${jwt.secret}")
    String secret;

    @Autowired
    private JwtTokenGenerator tokenGenerator;

    public String generateJwtAccessToken(JwtPayload payload) {
        return tokenGenerator.generateToken(payload);
    }

    public String generateJwtRefreshToken(JwtPayload payload) {
        //TODO: [azavgorodny] - not implemented yet
        return null;
    }

    public JwtPayload getPayload(String jwtToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(jwtToken)
                .getBody();
        return (JwtPayload) claims.get(JwtPayload.JWT_CLAIM_KEY);
    }

}
