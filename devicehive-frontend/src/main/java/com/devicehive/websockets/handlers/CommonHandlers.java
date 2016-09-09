package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
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

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class CommonHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @PreAuthorize("permitAll")
    public WebSocketResponse processServerInfo(WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        //TODO: Replace with timestamp service
        apiInfo.setServerTimestamp(new Date());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    public WebSocketResponse processAuthenticate(JsonObject request, WebSocketSession session) {
        String login = null;
        if (request.get("login") != null) {
            login = request.get("login").getAsString();
        }

        String password = null;
        if (request.get("password") != null) {
            password = request.get("password").getAsString();
        }

        String key = null;
        if (request.get("accessKey") != null) {
            try {
                key = request.get("accessKey").getAsString();
            } catch (UnsupportedOperationException e) {
                logger.error("Access Key is null");
            }
        }

        String deviceId = null;
        if (request.get("deviceId") != null) {
            deviceId = request.get("deviceId").getAsString();
        }

        logger.debug("authenticate action for {} ", login);
        HivePrincipal hivePrincipal = HiveWebsocketSessionState.get(session).getHivePrincipal();
        if (hivePrincipal != null && hivePrincipal.isAuthenticated()) {
            if (hivePrincipal.getUser() != null) {
                if (!hivePrincipal.getUser().getLogin().equals(login)) {
                    throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
                }
            } else if (hivePrincipal.getDevice() != null) {
                if (!hivePrincipal.getDevice().getGuid().equals(deviceId)) {
                    throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
                }
            } else if (!hivePrincipal.getKey().getKey().equals(key)) {
                throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
            }
        }

        HiveWebsocketSessionState state = (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);
        HiveAuthentication authentication;
        if (login != null) {
            authentication = authenticationManager.authenticateUser(login, password, details);
        } else if (key != null) {
            authentication = authenticationManager.authenticateKey(key, details);
        } else {
            throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
        }
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());

        return new WebSocketResponse();
    }
}
