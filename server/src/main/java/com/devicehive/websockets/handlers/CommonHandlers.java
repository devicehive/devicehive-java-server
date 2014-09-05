package com.devicehive.websockets.handlers;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.service.UserService;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.HiveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.websocket.Session;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public class CommonHandlers extends WebsocketHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private UserService userService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private AccessKeyService accessKeyService;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/serverinfo">WebSocket API:
     * Client: server/info</a>
     * Gets meta-information about the current API.
     *
     * @param session Current session
     * @return Json object with the following structure
     *         <code>
     *         {
     *         "action": {string},
     *         "status": {string},
     *         "requestId": {object},
     *         "info": {
     *         "apiVersion": {string},
     *         "serverTimestamp": {datetime},
     *         "restServerUrl": {string}
     *         }
     *         }
     *         </code>
     */

    @Action(value = "server/info")
    @PermitAll
    public WebSocketResponse processServerInfo(Session session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.REST_SERVER_URL);
        if (url != null) {
            apiInfo.setRestServerUrl(url);
        }
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/authenticate">WebSocket API:
     * Client: authenticate</a>
     * Authenticates a user.
     *
     * @param session Current session
     * @return JsonObject with structure
     *         <code>
     *         {
     *         "action": {string},
     *         "status": {string},
     *         "requestId": {object}
     *         }
     *         </code>
     *         Where:
     *         action - Action name: authenticate
     *         status - Operation execution status (success or error).
     *         requestId - Request unique identifier as specified in the request message.
     */
    @Action(value = "authenticate")
    @PermitAll
    public WebSocketResponse processAuthenticate(@WsParam("login") String login,
                                                 @WsParam("password") String password,
                                                 @WsParam("accessKey") String key,
                                                 @WsParam("deviceId") String deviceId,
                                                 @WsParam("deviceKey") String deviceKey,
                                                 Session session) {
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
        HiveWebsocketSessionState state = (HiveWebsocketSessionState)
                session.getUserProperties().get(HiveWebsocketSessionState.KEY);
        if (login != null && state.getEndpoint().equals(HiveEndpoint.CLIENT)) {
            User user = userService.authenticate(login, password);
            if (user != null) {
                hivePrincipal = new HivePrincipal(user, null, null);
            } else {
                throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
            }
        } else if (key != null && state.getEndpoint().equals(HiveEndpoint.CLIENT)) {
            AccessKey accessKey = accessKeyService.authenticate(key);
            if (accessKey != null) {
                hivePrincipal = new HivePrincipal(null, null, accessKey);
            } else {
                throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
            }
        } else if (deviceId != null && state.getEndpoint().equals(HiveEndpoint.DEVICE)) {
            Device device = deviceService.authenticate(deviceId, deviceKey);
            if (device != null) {
                hivePrincipal = new HivePrincipal(null, device, null);
            } else {
                throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
            }
        } else {
            throw new HiveException(Messages.INCORRECT_CREDENTIALS, SC_UNAUTHORIZED);
        }
        HiveWebsocketSessionState.get(session).setHivePrincipal(hivePrincipal);
        return new WebSocketResponse();
    }


}
