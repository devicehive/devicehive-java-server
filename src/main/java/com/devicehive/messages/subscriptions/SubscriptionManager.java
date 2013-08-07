package com.devicehive.messages.subscriptions;

import javax.inject.Singleton;

@Singleton
public class SubscriptionManager {

    private final CommandSubscriptionStorage commandSubscriptionStorage = new CommandSubscriptionStorage();

    private final CommandUpdateSubscriptionStorage commandUpdateSubscriptionStorage = new CommandUpdateSubscriptionStorage();

    private final NotificationSubscriptionStorage notificationSubscriptionStorage = new NotificationSubscriptionStorage();


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
