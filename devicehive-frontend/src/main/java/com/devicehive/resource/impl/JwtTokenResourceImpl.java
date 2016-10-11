package com.devicehive.resource.impl;

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Subnet;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.OAuthClientService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

@Service
public class JwtTokenResourceImpl implements JwtTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenResourceImpl.class);

    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private AccessKeyService accessKeyService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private OAuthClientService clientService;

    @Override
    public Response tokenRequest(String grantType, String accessKey, String username, String password) {
        JwtTokenVO tokenVO = new JwtTokenVO();
        JwtPayload.Builder jwtBuilder = JwtPayload.newBuilder();

        long currentTimeMillis = System.currentTimeMillis();
        //the JWT payload is valid for 20 minutes
        Date expirationDate = new Date(currentTimeMillis + Constants.DEFAULT_JWT_ACCESS_TOKEN_MAX_AGE);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //fixme
        switch (grantType) {
            case ACCESS_KEY:
                logger.debug("JwtToken: requesting access by access key");
                AccessKeyVO key = accessKeyService.authenticate(accessKey);

                if (key == null
                        || key.getUser() == null || !key.getUser().getStatus().equals(UserStatus.ACTIVE)
                        || (key.getExpirationDate() != null && key.getExpirationDate().before(timestampService.getDate()))) {
                    throw new BadCredentialsException("Unauthorized"); //"Wrong access key"
                }
                logger.debug("Access token authentication successful");

                Set<Long> allowedNetworksIds = new HashSet<>();
                Set<String> allowedDeviceGuids = new HashSet<>();
                Set<HiveAction> actions = new HashSet<>();
                Set<String> subnets = new HashSet<>();
                Set<String> domains = new HashSet<>();
                key.getPermissions().forEach(permission -> {
                    if (permission.getNetworkIdsAsSet() != null) {
                        allowedNetworksIds.addAll(permission.getNetworkIdsAsSet());
                    }

                    if (permission.getDeviceGuidsAsSet() != null) {
                        allowedDeviceGuids.addAll(permission.getDeviceGuidsAsSet());
                    }

                    Set<String> allowedActions = permission.getActionsAsSet();
                    if (permission.getActionsAsSet() != null)
                        allowedActions.forEach(action -> actions.add(HiveAction.fromString(action)));

                    Set<Subnet> allowedSubnets = permission.getSubnetsAsSet();
                    if (permission.getSubnetsAsSet() != null)
                        allowedSubnets.forEach(subnet -> subnets.add(subnet.toString()));

                    if (principal.getDomains() != null)
                        domains.addAll(principal.getDomains());
                });

                jwtBuilder.withPublicClaims(key.getUser().getId(), actions,
                                subnets,
                                domains,
                                allowedNetworksIds,
                                allowedDeviceGuids)
                        .withExpirationDate(expirationDate);

                tokenVO.setToken(tokenService.generateJwtAccessToken(jwtBuilder.buildAccessToken()));

                return ResponseFactory.response(OK, tokenVO, JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED);

            case REFRESH_TOKEN:
                logger.debug("JwtToken: requesting access by refresh token");
                JwtPayload jwtPayload = tokenService.getPayload(accessKey);
                if (jwtPayload.getType() != TokenType.REFRESH) {
                    return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_REQUEST_PARAMETERS));
                }

                jwtBuilder.withPublicClaims(jwtPayload.getUserId(),
                                jwtPayload.getActions(),
                                jwtPayload.getSubnets(),
                                jwtPayload.getDomains(),
                                jwtPayload.getNetworkIds(),
                                jwtPayload.getDeviceGuids())
                        .buildRefreshToken();

                tokenVO.setToken(tokenService.generateJwtRefreshToken(jwtPayload));

                return ResponseFactory.response(OK, tokenVO, JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED);

            case PASSWORD:
                logger.debug("Basic authentication requested for username {}", username);

                UserVO user = null;
                try {
                    user = userService.authenticate(username, password);
                } catch (HiveException e) {
                    logger.error("User auth failed", e);
                }
                if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                    String role = user.isAdmin() ? HiveRoles.ADMIN : HiveRoles.CLIENT;
                    logger.info("User {} authenticated with role {}", username, role);
                    Set<HiveAction> availableActions = user.isAdmin() ? AvailableActions.getAllHiveActions() :
                            AvailableActions.getClientHiveActions();

                    jwtBuilder.withUserId(user.getId()).withActions(availableActions)
                        .withExpirationDate(expirationDate);

                    tokenVO.setToken(tokenService.generateJwtAccessToken(jwtBuilder.buildAccessToken()));

                } else {
                    OAuthClientVO client = clientService.authenticate(username, password);
                    logger.info("oAuth client {} authenticated", username);
                    if (client != null) {
                        // TODO: Replace with OAuth client details
                        jwtBuilder.withUserId(client.getId()).withDomains(Collections.singleton(client.getDomain()))
                                .withSubnets(Collections.singleton(client.getSubnet()));

                        tokenVO.setToken(tokenService.generateJwtAccessToken(jwtBuilder.buildAccessToken()));
                    } else {
                        logger.warn("Basic auth for {} failed", username);
                        throw new BadCredentialsException("Invalid credentials");
                    }
                }

                return ResponseFactory.response(OK, tokenVO, JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED);
            default:
                return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_GRANT_TYPE));
        }
    }
}
