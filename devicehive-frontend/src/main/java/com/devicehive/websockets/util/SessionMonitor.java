package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.CommandGetSubscriptionRequest;
import com.devicehive.model.rpc.CommandGetSubscriptionResponse;
import com.devicehive.service.DeviceActivityService;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.handlers.CommandHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class SessionMonitor {
    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);

    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private DeviceActivityService deviceActivityService;

    private ConcurrentMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void registerSession(final WebSocketSession session) {
        sessionMap.put(session.getId(), session);
    }

    public WebSocketSession getSession(String sessionId) {
        WebSocketSession session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    public void removeSession(String sessionId) throws IOException {
        sessionMap.remove(sessionId);
        WebSocketSession session = sessionMap.get(sessionId);
        try {
            if (session != null) session.close();
        } catch (IOException ex) {
            logger.error("Error closing session", ex);
        }
    }

    public void updateDeviceSession(WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<String> authorizedDevices = principal != null ? principal.getDevices() : null;
        //Assumption is if principal is a device, it principal.getDevices will return only itself todo - update logic if neccessary
        if (authorizedDevices != null) {
            authorizedDevices.forEach(deviceGuid -> {
                deviceActivityService.update(deviceGuid);
            });
        }

        CopyOnWriteArraySet<String> subIds = (CopyOnWriteArraySet)
                session.getAttributes().get(CommandHandlers.SUBSCSRIPTION_SET_NAME);

        for (String id : subIds) {
            CommandGetSubscriptionRequest request = new CommandGetSubscriptionRequest(id);
            CompletableFuture<Response> future = new CompletableFuture<>();

            rpcClient.call(Request.newBuilder()
                    .withBody(request).build(), new ResponseConsumer(future));

            future.thenApply(r -> {
                Set<Subscription> subscriptions = ((CommandGetSubscriptionResponse) r.getBody()).getSubscriptions();

                for (Subscription subscription : subscriptions) {
                    if (subscription.getGuid() != null) {
                        deviceActivityService.update(subscription.getGuid());
                    }
                }
                return subscriptions;
            });
        }
    }

    @Scheduled(cron = "0/30 * * * * *")
    public synchronized void ping() {
        try {
            for (WebSocketSession session : sessionMap.values()) {
                if (session.isOpen()) {
                    logger.debug("Pinging session {}", session.getId());
                    session.sendMessage(new PingMessage());
                } else {
                    logger.debug("Session {} is closed.", session.getId());
                    sessionMap.remove(session.getId());
                }
            }
        } catch (IOException e) {
            logger.error("Exception while ping session");
        }
    }

    @PreDestroy
    public void closeAllSessions() {
        for (WebSocketSession session : sessionMap.values()) {
            try {
                session.close(CloseStatus.SERVICE_RESTARTED);
            } catch (IOException ex) {
                logger.error("Error closing session", ex);
            }
        }
        sessionMap.clear();
    }
}
