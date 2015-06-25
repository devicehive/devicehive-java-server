package com.devicehive.auth.websockets;

import com.devicehive.application.websocket.WebSocketAuthenticationManager;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.exceptions.HiveException;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.DEVICE_KEY;

@Aspect
@Component
@Order(0)
public class WebSocketActionAuthenticationAspect {

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Pointcut("execution(public * com.devicehive.websockets.handlers.WebsocketHandlers+.*(..))")
    public void publicHandlerMethod() {}

    @Pointcut("@annotation(com.devicehive.websockets.handlers.annotations.Action)")
    public void annotatedWithAction() {}

    @Before("publicHandlerMethod() && annotatedWithAction()")
    public void authenticate() throws Exception {
        WebSocketSession session = ThreadLocalVariablesKeeper.getSession();
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);

        HiveAuthentication authentication = (HiveAuthentication) session.getAttributes().get("authentication");
        //if not authenticated - authenticate as device or anonymous
        if (authentication == null || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            JsonObject request = ThreadLocalVariablesKeeper.getRequest();

            String deviceId = Optional.ofNullable(request.get(DEVICE_ID)).map(JsonElement::getAsString).orElse(null);
            String deviceKey = Optional.ofNullable(request.get(DEVICE_KEY)).map(JsonElement::getAsString).orElse(null);

            String action = Optional.ofNullable(request.get("action")).map(JsonElement::getAsString).orElse("");

            HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);
            if ((deviceId == null && deviceKey == null) || action.equals("device/save")) {
                authentication = authenticationManager.authenticateAnonymous(details);
            } else {
                try {
                    authentication = authenticationManager.authenticateDevice(deviceId, deviceKey, details);
                    state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
                } catch (AuthenticationException e) {
                    throw new HiveException(e.getMessage(), HttpStatus.UNAUTHORIZED.value());
                }
            }
            session.getAttributes().put("authentication", authentication);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
