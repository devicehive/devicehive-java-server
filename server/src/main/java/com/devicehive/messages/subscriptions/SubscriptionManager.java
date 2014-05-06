package com.devicehive.messages.subscriptions;


import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
public class SubscriptionManager {
    @EJB
    private CommandSubscriptionStorage commandSubscriptionStorage;
    @EJB
    private CommandUpdateSubscriptionStorage commandUpdateSubscriptionStorage;
    @EJB
    private NotificationSubscriptionStorage notificationSubscriptionStorage;

    public CommandSubscriptionStorage getCommandSubscriptionStorage() {
        return commandSubscriptionStorage;
    }

    public CommandUpdateSubscriptionStorage getCommandUpdateSubscriptionStorage() {
        return commandUpdateSubscriptionStorage;
    }

    public NotificationSubscriptionStorage getNotificationSubscriptionStorage() {
        return notificationSubscriptionStorage;
    }
}
