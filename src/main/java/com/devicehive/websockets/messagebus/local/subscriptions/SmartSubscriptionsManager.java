package com.devicehive.websockets.messagebus.local.subscriptions;


import javax.websocket.Session;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


abstract class SmartSubscriptionsManager extends SubscriptionsManager {

    private ConcurrentMap<UUID, Set<Session>> deviceNotificationMap = new ConcurrentHashMap<UUID, Set<Session>>();



    public SmartSubscriptionsManager() {
    }


    public void subscribe(Session clientSession, Collection<UUID> devices) {
        synchronized (clientSession) { //lock clientSession - all devices are to added atomically
            for (UUID dev : devices) {

                boolean added = false;

                while (! added) {
                    Set<Session> set = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
                    set.add(clientSession);

                    // try to add new set with one element
                    Set oldSet = deviceNotificationMap.putIfAbsent(dev, set);

                    if (oldSet != null) { // set was not added because there is already a set in hasmap

                        // lock set, it guarantees that set will be not removed from hashmap see @unsubscribe
                        synchronized (oldSet) {

                            /* check if old set is empty, empty means that it will be removed from hash map soon
                            *  we should not add new element to such set
                            */
                            if (!oldSet.isEmpty()) {
                                oldSet.add(clientSession);
                                added = true;
                            }
                        }
                    } else {
                        // newly created set was added
                        added = true;
                    }
                    // if !added then retry until added
                }
            }
        }
    }

    public void unsubscribe(Session clientSession, Collection<UUID> devices) {
        synchronized (clientSession) {
            for (UUID dev : devices) {
                Set set = deviceNotificationMap.get(dev);
                if (set != null) {

                    // lock set
                    synchronized (set) {

                        //remove element from set
                        set.remove(clientSession);

                        //remove both device key and sessions set from map if session set is empty
                        deviceNotificationMap.remove(dev, Collections.emptySet());
                    }
                }
            }
        }
    }


    public Set<Session> getSubscriptions(UUID device) {
        return deviceNotificationMap.get(device);
    }
}
