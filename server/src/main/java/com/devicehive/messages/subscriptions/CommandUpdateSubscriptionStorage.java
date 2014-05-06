package com.devicehive.messages.subscriptions;

import com.devicehive.websockets.util.SessionMonitor;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Set;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
public class CommandUpdateSubscriptionStorage extends AbstractStorage<Long, CommandUpdateSubscription> {

    @EJB
    private SessionMonitor sessionMonitor;

    public Set<CommandUpdateSubscription> getByCommandId(Long id) {
        return get(id);
    }


    public synchronized void removeBySession(String sessionId) {
        removeBySubscriptionId(sessionId);
    }

    public synchronized void removeByCommandId(Long commandId) {
        removeByEventSource(commandId);
    }

}
