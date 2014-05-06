package com.devicehive.messages.subscriptions;

import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
public class CommandSubscriptionStorage extends AbstractStorage<Long, CommandSubscription> {

    @EJB
    private SessionMonitor sessionMonitor;

    public Set<CommandSubscription> getByDeviceId(Long id) {
        return get(id);
    }

    @Deprecated
    public List<CommandSubscription> getBySession(String sessionId) {
        Set<UUID> subs = WebsocketSession.getCommandSubscriptions(sessionMonitor.getSession(sessionId));
        List<CommandSubscription> result = new ArrayList<>();
        for (UUID sub: subs){
            result.addAll(get(sub.toString()));
        }
        return result;
    }

    public Set<CommandSubscription> getBySubscriptionId(UUID subscriptionId){
        return get(subscriptionId.toString());
    }


    public synchronized void removeBySession(String sessionId) {
        Set<UUID> subs = WebsocketSession.getCommandSubscriptions(sessionMonitor.getSession(sessionId));
        for (UUID sub : subs) {
            removeBySubscriptionId(sub.toString());
        }
    }

    public synchronized void removeByDevice(Long deviceId) {
        removeByEventSource(deviceId);
    }

}
