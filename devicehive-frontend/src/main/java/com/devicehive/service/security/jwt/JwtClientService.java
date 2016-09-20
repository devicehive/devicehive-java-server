package com.devicehive.service.security.jwt;

import com.devicehive.security.jwt.JwtPrincipal;
import com.devicehive.security.util.JwtTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class responsible for access and refresh JWT keys generation.
 */
@Component
public class JwtClientService {


    @Autowired
    private JwtTokenGenerator tokenGenerator;


    public String generateJwtAccessToken(JwtPrincipal principal) {
        return tokenGenerator.generateToken(principal);
    }

    public String generateJwtRefreshToken() {
        //TODO: [azavgorodny] - not implemented yet
        return null;
    }


}
