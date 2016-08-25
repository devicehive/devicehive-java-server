package com.devicehive.auth.websockets;

import com.devicehive.websockets.WebSocketAuthenticationManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Aspect
@Component
@Order(0)
public class WebSocketActionAuthenticationAspect {

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Pointcut("execution(public * com.devicehive.websockets.handlers..*(..)) && args(session,..)")
    public void publicHandlerMethod(WebSocketSession session) {}

    @Before("publicHandlerMethod(session)")
    public void authenticate(WebSocketSession session) throws Exception {
        //TODO: Implement authentication
//        HiveAuthentication authentication = (HiveAuthentication) session.getAttributes()
//                .get(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION);
//
//        //if not authenticated - authenticate as device or anonymous
//        if (authentication == null || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
//            HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);
//            authentication = authenticationManager.authenticateAnonymous(details);
//            session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);
//        }
//        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
