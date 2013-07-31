package com.devicehive.messages.data;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.devicehive.messages.data.hash.HashBasedDataSource;
import com.devicehive.messages.data.hash.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

public class MessagesDataSourceTest {

    /* 
     * TODO: 
     * 1. Add support for DerbyDataSource. 
     * 2. Add comparision of hashMap vs derby - should have the same result. 
     * */
    private static HashBasedDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        dataSource = new HashBasedDataSource();

        NotificationSubscriptionDAO notificationSubscriptionDAO = new NotificationSubscriptionDAO();
        CommandSubscriptionDAO commandSubscriptionDAO = new CommandSubscriptionDAO();
        CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO = new CommandUpdatesSubscriptionDAO();

        Field f1 = dataSource.getClass().getDeclaredField("notificationSubscriptionDAO");
        f1.setAccessible(true);
        f1.set(dataSource, notificationSubscriptionDAO);

        Field f2 = dataSource.getClass().getDeclaredField("commandSubscriptionDAO");
        f2.setAccessible(true);
        f2.set(dataSource, commandSubscriptionDAO);

        Field f3 = dataSource.getClass().getDeclaredField("commandUpdatesSubscriptionDAO");
        f3.setAccessible(true);
        f3.set(dataSource, commandUpdatesSubscriptionDAO);
    }

    @Test
    public void testCommandSubscriptions() {
        /* 
         * Case 1:
         * 1. Device sends 'command/subscribe' message.
         * 2. New CommandSubscription added to dataSource
         * 3. We can ask (query) for this subscription
         * 
         * Case 2:   
         * 1. Device sends 'command/unsubscribe' message
         * 2. Subscription removed from dataSource
         * 3. We can't ask (query) for this subscription
         * 
         * Case 3:
         * 1. Device sends 'command/subscribe' message.
         * 2. New CommandSubscription added to dataSource
         * 3. Device closes session
         * 4. Subscription removed from dataSource
         * 5. We can't ask (query) for this subscription
         */

        /* ---------------- Common ---------------- */
        String sessionId = UUID.randomUUID().toString();
        Long deviceId = Long.valueOf(new Random().nextLong());
        String sessionId_2 = UUID.randomUUID().toString();
        Long deviceId_2 = Long.valueOf(new Random().nextLong());
        /* ---------------- Common ---------------- */

        /* ---------------- Case 1 ---------------- */
        dataSource.addCommandsSubscription(sessionId, deviceId);
        CommandsSubscription object = dataSource.commandSubscriptions().getByDeviceId(deviceId);
        assertNotNull(object);
        assertEquals(sessionId, object.getSessionId());
        assertEquals(deviceId, object.getDeviceId());

        dataSource.addCommandsSubscription(sessionId_2, deviceId_2);
        CommandsSubscription object_2 = dataSource.commandSubscriptions().getByDeviceId(deviceId_2);
        assertNotNull(object_2);
        assertEquals(sessionId_2, object_2.getSessionId());
        assertEquals(deviceId_2, object_2.getDeviceId());

        assertNotEquals(object, object_2);
        /* ---------------- Case 1 ---------------- */

        /* ---------------- Case 2 ---------------- */
        dataSource.removeCommandsSubscription(deviceId);
        object = dataSource.commandSubscriptions().getByDeviceId(deviceId);
        assertNull(object);
        /* ---------------- Case 2 ---------------- */

        /* ---------------- Case 3 ---------------- */
        dataSource.removeDeviceSubscriptions(sessionId_2);
        object = dataSource.commandSubscriptions().getByDeviceId(deviceId_2);
        assertNull(object);
        /* ---------------- Case 3 ---------------- */

    }

    @Test
    public void testCommandUpdateSubscription() {
        /*
         * Case 1:
         * 1. Client sends 'command/insert' message
         * 2. New CommandUpdateSubscription added to dataSource
         * 3. We can ask (query) for this subscription
         * 
         * Case 2:
         * 1. Client sends another 'command/insert' message
         * 2. New CommandUpdateSubscription added to dataSource
         * 3. We can ask (query) for this subscription and for previous subscription
         * 
         * Case 3:
         * 1. Client closes session
         * 2. Subscription removed from dataSource
         * 3. We can't ask (query) for this subscription
         */

        /* ---------------- Common ---------------- */
        String sessionId = UUID.randomUUID().toString();
        Long commandId = Long.valueOf(new Random().nextLong());
        String sessionId_2 = UUID.randomUUID().toString();
        Long commandId_2 = Long.valueOf(new Random().nextLong());
        /* ---------------- Common ---------------- */

        /* ---------------- Case 1 & 2 ---------------- */
        dataSource.addCommandUpdatesSubscription(sessionId, commandId);
        CommandUpdatesSubscription object = dataSource.commandUpdatesSubscriptions().getByCommandId(commandId);
        assertNotNull(object);
        assertEquals(sessionId, object.getSessionId());
        assertEquals(commandId, object.getCommandId());

        dataSource.addCommandUpdatesSubscription(sessionId_2, commandId_2);
        CommandUpdatesSubscription object_2 = dataSource.commandUpdatesSubscriptions().getByCommandId(commandId_2);
        assertNotNull(object_2);
        assertEquals(sessionId_2, object_2.getSessionId());
        assertEquals(commandId_2, object_2.getCommandId());

        assertNotEquals(object, object_2);

        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);
        CommandUpdatesSubscription object_1_2 = dataSource.commandUpdatesSubscriptions().getByCommandId(commandId_2);
        assertNotNull(object_1_2);
        assertEquals(sessionId, object_1_2.getSessionId());
        assertEquals(commandId_2, object_1_2.getCommandId());

        assertEquals(object_1_2.getCommandId(), object_2.getCommandId());
        assertNotEquals(object_1_2.getSessionId(), object_2.getSessionId());
        /* ---------------- Case 1 & 2 ---------------- */

        /* ---------------- Case 3 ---------------- */
        dataSource.removeClientSubscriptions(sessionId);
        object = dataSource.commandUpdatesSubscriptions().getByCommandId(commandId);
        assertNull(object);

        dataSource.removeClientSubscriptions(sessionId_2);
        object = dataSource.commandUpdatesSubscriptions().getByCommandId(commandId_2);
        assertNull(object);
        /* ---------------- Case 3 ---------------- */
    }

    @Test
    public void testNotificationSubscription() {
        /*
         * Case 1:
         * 1. Client sends 'notification/subscribe' message
         * 2. New NotificationSubscription added to dataSource
         * 3. We can ask (query) for this subscription
         * 
         * Case 2:
         * 1. Client sends 'notification/unsubscribe' message
         * 2. Subscription removed from dataSource
         * 3. We can't ask (query) for this subscription
         * 
         * Case 3:
         * 1. Client sends 'notification/subscribe' message for null device
         * 2. New NotificationSubscription added to dataSource
         * 3. We can ask (query) for this subscription
         * 
         *  Case 4:
         * 1. Client sends 'notification/unsubscribe' message for null device
         * 2. Subscription removed from dataSource
         * 3. We can't ask (query) for this subscription
         * 
         * Case 5:
         * 1. Client closes session
         * 2. Subscription removed from dataSource
         * 3. We can't ask (query) for this subscription
         */

        /* ---------------- Common ---------------- */
        String sessionId = UUID.randomUUID().toString();
        String sessionId_2 = UUID.randomUUID().toString();
        Long deviceId = Long.valueOf(new Random().nextLong());
        Long deviceIdNull = null;
        /* ---------------- Common ---------------- */

        /* ---------------- Case 1 ---------------- */
        dataSource.addNotificationsSubscription(sessionId, Arrays.asList(deviceId));
        List<String> sList = dataSource.notificationSubscriptions().getSessionIdSubscribedByDevice(deviceId);
        assertNotNull(sList);
        assertEquals(1, sList.size());
        assertEquals(sessionId, sList.get(0));

        dataSource.addNotificationsSubscription(sessionId_2, Arrays.asList(deviceId));
        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedByDevice(deviceId);
        assertNotNull(sList);
        assertEquals(2, sList.size());
        assertTrue(sList.contains(sessionId));
        assertTrue(sList.contains(sessionId_2));
        /* ---------------- Case 1 ---------------- */

        /* ---------------- Case 2 ---------------- */
        dataSource.removeNotificationSubscription(deviceId);
        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedByDevice(deviceId);
        assertNotNull(sList);
        assertTrue(sList.isEmpty());

        dataSource.addNotificationsSubscription(sessionId, Arrays.asList(deviceId));
        dataSource.addNotificationsSubscription(sessionId_2, Arrays.asList(deviceId));
        dataSource.removeNotificationSubscriptions(sessionId, Arrays.asList(deviceId));

        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedByDevice(deviceId);
        assertNotNull(sList);
        assertEquals(1, sList.size());
        assertTrue(sList.contains(sessionId_2));
        /* ---------------- Case 2 ---------------- */

        /* ---------------- Case 3 ---------------- */
        dataSource.addNotificationsSubscription(sessionId, Arrays.asList(deviceIdNull));
        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedForAll();
        assertNotNull(sList);
        assertEquals(1, sList.size());
        assertEquals(sessionId, sList.get(0));

        dataSource.addNotificationsSubscription(sessionId_2, Arrays.asList(deviceIdNull));
        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedForAll();
        assertNotNull(sList);
        assertEquals(2, sList.size());
        assertTrue(sList.contains(sessionId));
        assertTrue(sList.contains(sessionId_2));
        /* ---------------- Case 3 ---------------- */

        /* ---------------- Case 4 ---------------- */
        dataSource.removeNotificationSubscriptions(sessionId, Arrays.asList(deviceIdNull));
        sList = dataSource.notificationSubscriptions().getSessionIdSubscribedForAll();
        assertNotNull(sList);
        assertEquals(1, sList.size());
        assertEquals(sessionId_2, sList.get(0));
        /* ---------------- Case 4 ---------------- */
    }

}
