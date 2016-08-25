package com.devicehive.websockets.handlers;

import com.devicehive.configuration.Constants;
import com.devicehive.model.ApiInfoVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

@Component
public class CommonHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

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

    @PreAuthorize("permitAll")
    public WebSocketResponse processAuthenticate(JsonObject request,
                                                 WebSocketSession session) {
        return new WebSocketResponse();
    }
}
