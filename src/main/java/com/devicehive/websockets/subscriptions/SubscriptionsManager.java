package com.devicehive.websockets.subscriptions;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 11.06.13
 * Time: 10:44
 * To change this template use File | Settings | File Templates.
 */
interface SubscriptionsManager<S> {
    void subscribe(S clientSession, long... devices);

    void unsubscribe(S clientSession, long... devices);

    Set<S> getSubscriptions(long device);

}
