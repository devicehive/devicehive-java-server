package com.devicehive.websockets.messagebus;

import java.util.Set;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 11.06.13
 * Time: 10:44
 * To change this template use File | Settings | File Templates.
 */
interface SubscriptionsManager<S> {
    void subscribe(S clientSession, UUID... devices);

    void unsubscribe(S clientSession, UUID... devices);

    Set<S> getSubscriptions(UUID device);

}
