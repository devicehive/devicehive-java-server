package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.WebSocketAuthenticationManager;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

@Component
public class CommonHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Autowired
    private TimestampService timestampService;


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
        if (request.get("token") != null) {
            jwtToken = request.get("token").getAsString();
        }

        HiveWebsocketSessionState state = (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);

        HiveAuthentication authentication = authenticationManager.authenticateJWT(jwtToken, details);

        HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        authentication.setHivePrincipal(principal);

        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal(principal);

        return new WebSocketResponse();
    }
}
