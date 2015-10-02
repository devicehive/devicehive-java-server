package com.devicehive.auth.websockets;

import com.devicehive.application.websocket.WebSocketAuthenticationManager;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Aspect
@Component
@Order(0)
public class WebSocketActionAuthenticationAspect {

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Pointcut("execution(public * com.devicehive.websockets.handlers.WebsocketHandlers+.*(..))")
    public void publicHandlerMethod() {}

    @Pointcut("execution(public * com.devicehive.websockets.handlers.NotificationHandlers.*(..))")
    public void notNotification() {}

    @Pointcut("@annotation(com.devicehive.websockets.handlers.annotations.Action)")
    public void annotatedWithAction() {}

    @Before("publicHandlerMethod() && annotatedWithAction()")
    public void authenticate() throws Exception {
        WebSocketSession session = ThreadLocalVariablesKeeper.getSession();

        HiveAuthentication authentication = (HiveAuthentication) session.getAttributes().get("authentication");
        //if not authenticated - authenticate as device or anonymous
        if (authentication == null || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);
            authentication = authenticationManager.authenticateAnonymous(details);
            session.getAttributes().put("authentication", authentication);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
