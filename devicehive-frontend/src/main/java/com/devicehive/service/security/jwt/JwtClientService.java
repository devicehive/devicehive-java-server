package com.devicehive.service.security.jwt;

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.util.JwtTokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
        LinkedHashMap payloadMap = (LinkedHashMap) claims.get(JwtPayload.JWT_CLAIM_KEY);

        Optional userId = Optional.ofNullable(payloadMap.get(JwtPayload.USER_ID));
        Optional networkIds = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.NETWORK_IDS));
        Optional actions = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.ACTIONS));
        Optional deviceGuids = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.DEVICE_GUIDS));
        Optional expiration = Optional.ofNullable((Long) payloadMap.get(JwtPayload.EXPIRATION));

        JwtPayload.Builder builder = new JwtPayload.Builder();
        if (userId.isPresent()) builder.withUserId(Long.valueOf(userId.get().toString()));
        if (networkIds.isPresent()) builder.withNetworkIds(new HashSet<>((ArrayList) networkIds.get()));
        if (actions.isPresent()) builder.withActions(new HashSet<>((ArrayList) actions.get()));
        if (deviceGuids.isPresent()) builder.withDeviceGuids(new HashSet<>((ArrayList) deviceGuids.get()));
        if (expiration.isPresent()) builder.withExpirationDate(new Date((Long) expiration.get()));
        return  builder.buildPayload();
    }

}
