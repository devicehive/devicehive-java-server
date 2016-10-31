package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AvailableActions;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.WebSocketAuthenticationManager;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class CommonHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private JwtClientService jwtClientService;

    @PreAuthorize("permitAll")
    public WebSocketResponse processServerInfo(WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        //TODO: Replace with timestamp service
        apiInfo.setServerTimestamp(timestampService.getDate());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    //TODO - replace with jwt authentication
    @PreAuthorize("permitAll")
    public WebSocketResponse processAuthenticate(JsonObject request, WebSocketSession session) {

        String jwtToken = null;
        JwtPayload jwtPayload = null;
        if (request.get("token") != null) {
            jwtToken = request.get("token").getAsString();
            try {
                jwtPayload = jwtClientService.getPayload(jwtToken);
            } catch (MalformedJwtException e) {
                logger.error(e.getMessage(), e);
                throw new BadCredentialsException("Unauthorized");
            }
        }

        HivePrincipal hivePrincipal = HiveWebsocketSessionState.get(session).getHivePrincipal();
        if (hivePrincipal != null && hivePrincipal.isAuthenticated()) {
            if (hivePrincipal.getUser() != null) {
                if (!hivePrincipal.getUser().getLogin().equals(jwtPayload.getUserId().toString()))
                    throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
            }
        }

        HiveWebsocketSessionState state = (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);

        HiveAuthentication authentication = authenticationManager.authenticateJWT(jwtToken, details);

        HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        if (jwtPayload == null
                || (jwtPayload.getExpiration() != null && jwtPayload.getExpiration().before(timestampService.getDate()))) {
            throw new BadCredentialsException("Unauthorized");
        }
        logger.debug("Jwt token authentication successful");
        if (jwtPayload.getUserId() != null) {
            UserVO userVO = userService.findById(jwtPayload.getUserId());
            principal.setUser(userVO);
        }
        Set<String> networkIds = jwtPayload.getNetworkIds();
        if (networkIds != null) {
            if (networkIds.contains("*")) {
                principal.setAllNetworksAvailable(true);
            } else {
                principal.setNetworkIds(networkIds.stream().map(Long::valueOf).collect(Collectors.toSet()));
            }
        }

        Set<String> deviceGuids = jwtPayload.getDeviceGuids();
        if (deviceGuids != null) {
            if (deviceGuids.contains("*")) {
                principal.setAllDevicesAvailable(true);
            } else {
                principal.setDeviceGuids(deviceGuids);
            }
        }

        Set<String> availableActions = jwtPayload.getActions();
        if (availableActions != null) {
            if (availableActions.contains("*")) {
                principal.setActions(AvailableActions.getAllHiveActions());
            } else if (availableActions.isEmpty()) {
                principal.setActions(AvailableActions.getClientHiveActions());
            } else {
                principal.setActions(availableActions.stream().map(HiveAction::fromString).collect(Collectors.toSet()));
            }
        }

        authentication.setHivePrincipal(principal);

        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal(principal);

        return new WebSocketResponse();
    }
}
