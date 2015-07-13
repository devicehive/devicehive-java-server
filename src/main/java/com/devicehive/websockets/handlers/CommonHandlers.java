package com.devicehive.websockets.handlers;


import com.devicehive.application.websocket.WebSocketAuthenticationManager;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ApiInfo;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.HiveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class CommonHandlers extends WebsocketHandlers {
    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private UserService userService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private AccessKeyService accessKeyService;

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/serverinfo">WebSocket API:
     * Client: server/info</a> Gets meta-information about the current API.
     *
     * @param session Current session
     * @return Json object with the following structure <code> { "action": {string}, "status": {string}, "requestId":
     *         {object}, "info": { "apiVersion": {string}, "serverTimestamp": {datetime}, "restServerUrl": {string} } }
     *         </code>
     */

    @Action(value = "server/info")
    @PreAuthorize("permitAll")
    public WebSocketResponse processServerInfo(WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/authenticate">WebSocket API:
     * Client: authenticate</a> Authenticates a user.
     *
     * @param session Current session
     * @return JsonObject with structure <code> { "action": {string}, "status": {string}, "requestId": {object} }
     *         </code> Where: action - Action name: authenticate status - Operation execution status (success or error).
     *         requestId - Request unique identifier as specified in the request message.
     */
    @Action(value = "authenticate")
    @PreAuthorize("permitAll")
    public WebSocketResponse processAuthenticate(@WsParam("login") String login,
                                                 @WsParam("password") String password,
                                                 @WsParam("accessKey") String key,
                                                 @WsParam("deviceId") String deviceId,
                                                 @WsParam("deviceKey") String deviceKey,
                                                 WebSocketSession session) {
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
        if (login != null && state.getEndpoint().equals(HiveEndpoint.CLIENT)) {
            authentication = authenticationManager.authenticateUser(login, password, details);
            session.getAttributes().put("authentication", authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        } else if (key != null && state.getEndpoint().equals(HiveEndpoint.CLIENT)) {
            authentication = authenticationManager.authenticateKey(key, details);
            session.getAttributes().put("authentication", authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        } else if (deviceId != null && state.getEndpoint().equals(HiveEndpoint.DEVICE)) {
            authentication = authenticationManager.authenticateDevice(deviceId, deviceKey, details);
            session.getAttributes().put("authentication", authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        } else {
            throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
        }
        session.getAttributes().put("authentication", authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        return new WebSocketResponse();
    }


}
