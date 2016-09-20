package com.devicehive.service.security.jwt;

import com.devicehive.dao.AccessKeyDao;
import com.devicehive.security.jwt.JwtPrincipal;
import com.devicehive.security.util.JwtTokenGenerator;
import com.devicehive.vo.AccessKeyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Class responsible for access and refresh JWT keys generation.
 */
@Component
public class JwtClientService {

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Autowired
    private JwtTokenGenerator tokenGenerator;


    public String generateJwtAccessToken(JwtPrincipal principal) {
        return tokenGenerator.generateToken(principal);
    }

    public String generateJwtRefreshToken() {
        //TODO: [azavgorodny] - not implemented yet
        return null;
    }

    @Transactional
    public AccessKeyVO getAccessKey(@NotNull String key) {
        Optional<AccessKeyVO> accessKeyOpt = accessKeyDao.getByKey(key);

        if (!accessKeyOpt.isPresent()) {
            return null;
        }
        AccessKeyVO accessKey = accessKeyOpt.get();
        return accessKey;
    }

}
