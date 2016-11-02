package com.devicehive.service.security.jwt;

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.security.util.JwtTokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

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
        return tokenGenerator.generateToken(payload, TokenType.ACCESS);
    }

    public String generateJwtRefreshToken(JwtPayload payload) {
        return tokenGenerator.generateToken(payload, TokenType.REFRESH);
    }

    public JwtPayload getPayload(String jwtToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(jwtToken)
                .getBody();
        LinkedHashMap payloadMap = (LinkedHashMap) claims.get(JwtPayload.JWT_CLAIM_KEY);

        Optional userId = Optional.ofNullable(payloadMap.get(JwtPayload.USER_ID));
        Optional networkIds = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.NETWORK_IDS));
        Optional actions = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.ACTIONS));
        Optional deviceGuids = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.DEVICE_GUIDS));
        Optional expiration = Optional.ofNullable(payloadMap.get(JwtPayload.EXPIRATION));
        Optional tokenType = Optional.ofNullable(payloadMap.get(JwtPayload.TOKEN_TYPE));

        JwtPayload.Builder builder = new JwtPayload.Builder();
        if (userId.isPresent()) builder.withUserId(Long.valueOf(userId.get().toString()));
        if (networkIds.isPresent()) builder.withNetworkIds(new HashSet<>((ArrayList) networkIds.get()));
        if (actions.isPresent()) builder.withActions(new HashSet<>((ArrayList) actions.get()));
        if (deviceGuids.isPresent()) builder.withDeviceGuids(new HashSet<>((ArrayList) deviceGuids.get()));
        if (tokenType.isPresent())
            builder.withTokenType(TokenType.valueOf((String)tokenType.get()));
        else
            throw new MalformedJwtException("Token type should be provided in the token");
        if (expiration.isPresent())
            builder.withExpirationDate(new Date((Long)expiration.get()));
        else
            throw new MalformedJwtException("Expiration date should be provided in the token");
        return builder.buildPayload();
    }

}
