package com.devicehive.messages.subscriptions;


import javax.ejb.ConcurrencyManagement;
import javax.ejb.Singleton;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
public class SubscriptionManager {

    private final CommandSubscriptionStorage commandSubscriptionStorage = new CommandSubscriptionStorage();

    private final NotificationSubscriptionStorage notificationSubscriptionStorage =
            new NotificationSubscriptionStorage();


    public CommandSubscriptionStorage getCommandSubscriptionStorage() {
        return commandSubscriptionStorage;
    }

    public NotificationSubscriptionStorage getNotificationSubscriptionStorage() {
        return notificationSubscriptionStorage;
    }
}
