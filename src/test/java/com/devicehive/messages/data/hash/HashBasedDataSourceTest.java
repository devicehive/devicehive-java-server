package com.devicehive.messages.data.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.devicehive.messages.data.hash.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.devicehive.messages.data.subscriptions.model.NotificationsSubscription;

public class HashBasedDataSourceTest {

    private static HashBasedDataSource dataSource;

    @Before
    public void setUpBeforeClass() throws Exception {
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

    @SuppressWarnings("unchecked")
    @Test
    public void testAddCommandsSubscription() throws Exception {
        String sessionId = null;
        Long deviceId = Long.valueOf(new Random().nextLong());
        dataSource.addCommandsSubscription(sessionId, deviceId);

        Field mapF = CommandSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);

        Map<String, CommandsSubscription> sessionMap = (Map<String, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());
        assertNull(sessionMap.get(sessionId));

        /*---------------*/

        sessionId = UUID.randomUUID().toString();
        dataSource.addCommandsSubscription(sessionId, deviceId);

        CommandsSubscription entity = sessionMap.get(sessionId);
        assertNotNull(entity);
        assertEquals(new CommandsSubscription(deviceId, sessionId), entity);

        /*---------------*/

        Long deviceId_2 = Long.valueOf(new Random().nextLong());
        dataSource.addCommandsSubscription(sessionId, deviceId_2);
        dataSource.addCommandsSubscription(sessionId, deviceId_2);
        dataSource.addCommandsSubscription(sessionId, deviceId_2);

        entity = sessionMap.get(sessionId);
        assertNotNull(entity);
        assertEquals(new CommandsSubscription(deviceId_2, sessionId), entity);

        /*---------------*/

        mapF = CommandSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, CommandsSubscription> deviceMap = (Map<Long, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());

        assertNotNull(deviceMap.get(deviceId));
        assertNotNull(deviceMap.get(deviceId_2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveCommandsSubscriptionStringLong() throws Exception {
        String sessionId = null;
        Long deviceId = Long.valueOf(new Random().nextLong());
        dataSource.addCommandsSubscription(sessionId, deviceId);

        Field mapF = CommandSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);

        Map<String, CommandsSubscription> sessionMap = (Map<String, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());
        assertNull(sessionMap.get(sessionId));

        dataSource.removeCommandsSubscription(sessionId, deviceId);
        assertNull(sessionMap.get(sessionId));

        /*---------------*/

        sessionId = UUID.randomUUID().toString();
        dataSource.addCommandsSubscription(sessionId, deviceId);
        assertNotNull(sessionMap.get(sessionId));
        dataSource.removeCommandsSubscription(sessionId, deviceId);
        assertNull(sessionMap.get(sessionId));

        /*---------------*/

        Long d1 = Long.valueOf(new Random().nextLong());
        Long d2 = Long.valueOf(new Random().nextLong());

        dataSource.addCommandsSubscription(sessionId, d1);
        dataSource.addCommandsSubscription(sessionId, d2);

        dataSource.removeCommandsSubscription(sessionId, d1);
        assertNull(sessionMap.get(sessionId));

        dataSource.removeCommandsSubscription(null, d1);
        assertNull(sessionMap.get(sessionId));

        dataSource.removeCommandsSubscription(sessionId, d2);
        assertNull(sessionMap.get(sessionId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveCommandsSubscriptionLong() throws Exception {
        String sessionId_1 = UUID.randomUUID().toString();
        String sessionId_2 = UUID.randomUUID().toString();
        Long deviceId = Long.valueOf(new Random().nextLong());

        Field mapF = CommandSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<String, CommandsSubscription> sessionMap = (Map<String, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());

        mapF = CommandSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, CommandsSubscription> deviceMap = (Map<Long, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());

        dataSource.addCommandsSubscription(sessionId_1, deviceId);
        dataSource.addCommandsSubscription(sessionId_2, deviceId);
        assertNull(sessionMap.get(sessionId_1));
        assertNotNull(sessionMap.get(sessionId_2));
        assertNotNull(deviceMap.get(deviceId));

        dataSource.removeCommandsSubscription(deviceId);
        assertNull(sessionMap.get(sessionId_2));

        assertNull(deviceMap.get(deviceId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveDeviceSubscriptions() throws Exception {
        String sessionId_1 = UUID.randomUUID().toString();
        Long deviceId_1 = Long.valueOf(new Random().nextLong());

        String sessionId_2 = UUID.randomUUID().toString();
        Long deviceId_2 = Long.valueOf(new Random().nextLong());

        Field mapF = CommandSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<String, CommandsSubscription> sessionMap = (Map<String, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());

        mapF = CommandSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, CommandsSubscription> deviceMap = (Map<Long, CommandsSubscription>) mapF.get(dataSource.commandSubscriptions());

        dataSource.addCommandsSubscription(sessionId_1, deviceId_1);
        dataSource.addCommandsSubscription(sessionId_2, deviceId_2);

        dataSource.removeDeviceSubscriptions(sessionId_1);

        dataSource.removeDeviceSubscriptions(sessionId_2);

        assertNull(sessionMap.get(sessionId_1));
        assertNull(sessionMap.get(sessionId_2));

        assertNull(deviceMap.get(deviceId_1));
        assertNull(deviceMap.get(deviceId_2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddCommandUpdatesSubscription() throws Exception {
        String sessionId = null;
        Long commandId = Long.valueOf(new Random().nextLong());
        dataSource.addCommandUpdatesSubscription(sessionId, commandId);

        Field mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<String, CommandUpdatesSubscription> sessionMap = (Map<String, CommandUpdatesSubscription>) mapF.get(dataSource
                .commandUpdatesSubscriptions());

        assertNull(sessionMap.get(sessionId));

        /*---------------*/

        sessionId = UUID.randomUUID().toString();
        dataSource.addCommandUpdatesSubscription(sessionId, commandId);

        Set<CommandUpdatesSubscription> set = (Set<CommandUpdatesSubscription>) sessionMap.get(sessionId);
        assertNotNull(set);
        assertEquals(new CommandUpdatesSubscription(commandId, sessionId), set.iterator().next());

        /*---------------*/

        Long commandId_2 = Long.valueOf(new Random().nextLong());
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);

        set = (Set<CommandUpdatesSubscription>) sessionMap.get(sessionId);
        assertEquals(2, set.size());

        /*---------------*/

        mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("commandToObject");
        mapF.setAccessible(true);
        Map<Long, CommandUpdatesSubscription> commandMap = (Map<Long, CommandUpdatesSubscription>) mapF.get(dataSource.commandUpdatesSubscriptions());

        assertNotNull(commandMap.get(commandId));
        assertNotNull(commandMap.get(commandId_2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveCommandUpdatesSubscriptionLong() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        Long commandId_1 = Long.valueOf(new Random().nextLong());
        Long commandId_2 = Long.valueOf(new Random().nextLong());
        Long commandId_3 = Long.valueOf(new Random().nextLong());

        dataSource.addCommandUpdatesSubscription(sessionId, commandId_1);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_3);

        Field mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<String, CommandUpdatesSubscription> sessionMap = (Map<String, CommandUpdatesSubscription>) mapF.get(dataSource
                .commandUpdatesSubscriptions());
        mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("commandToObject");
        mapF.setAccessible(true);
        Map<Long, CommandUpdatesSubscription> commandMap = (Map<Long, CommandUpdatesSubscription>) mapF.get(dataSource.commandUpdatesSubscriptions());

        dataSource.removeCommandUpdatesSubscription(commandId_1);
        dataSource.removeCommandUpdatesSubscription(commandId_2);
        dataSource.removeCommandUpdatesSubscription(commandId_3);

        assertNull(commandMap.get(commandId_1));
        assertNull(commandMap.get(commandId_2));
        assertNull(commandMap.get(commandId_3));

        assertTrue(((Set) sessionMap.get(sessionId)).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveClientSubscriptions() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        Long commandId_1 = Long.valueOf(new Random().nextLong());
        Long commandId_2 = Long.valueOf(new Random().nextLong());
        Long commandId_3 = Long.valueOf(new Random().nextLong());

        dataSource.addCommandUpdatesSubscription(sessionId, commandId_1);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_2);
        dataSource.addCommandUpdatesSubscription(sessionId, commandId_3);

        Field mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<String, CommandUpdatesSubscription> sessionMap = (Map<String, CommandUpdatesSubscription>) mapF.get(dataSource
                .commandUpdatesSubscriptions());
        mapF = CommandUpdatesSubscriptionDAO.class.getDeclaredField("commandToObject");
        mapF.setAccessible(true);
        Map<Long, CommandUpdatesSubscription> commandMap = (Map<Long, CommandUpdatesSubscription>) mapF.get(dataSource.commandUpdatesSubscriptions());

        dataSource.removeClientSubscriptions(sessionId);

        assertNull(commandMap.get(commandId_1));
        assertNull(commandMap.get(commandId_2));
        assertNull(sessionMap.get(sessionId));
        assertNull(commandMap.get(commandId_3));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddNotificationsSubscription() throws Exception {
        /* Session = ok, Ids = ok */
        String sessionId = UUID.randomUUID().toString();
        List<Long> ids = new ArrayList<>();
        ids.add(Long.valueOf(1L));
        ids.add(Long.valueOf(2L));
        ids.add(Long.valueOf(3L));

        dataSource.addNotificationsSubscription(sessionId, ids);

        Field mapF = NotificationSubscriptionDAO.class.getDeclaredField("keyToObject");
        mapF.setAccessible(true);
        Map keyMap = (Map) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> deviceMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> sessionMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());

        Class<?> c = NotificationSubscriptionDAO.class.getClassLoader().loadClass(
                "com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO$Key");
        Constructor lego = c.getConstructor(NotificationSubscriptionDAO.class, Long.class, String.class);
        lego.setAccessible(true);
        Object key = lego.newInstance(dataSource.notificationSubscriptions(), ids.get(0), sessionId);

        assertTrue(keyMap.keySet().contains(key));

        /*--------------*/

        Set set = (Set) sessionMap.get(sessionId);
        assertNotNull(set);
        assertEquals(ids.size(), set.size());

        /*--------------*/

        set = (Set) deviceMap.get(ids.get(0));
        assertNotNull(set);
        assertEquals(1, set.size());

        set = (Set) deviceMap.get(ids.get(1));
        assertNotNull(set);
        assertEquals(1, set.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddNotificationsSubscriptionNull() throws Exception {
        /* Session = ok, Ids = null */
        String sessionId = UUID.randomUUID().toString();

        dataSource.addNotificationsSubscription(sessionId, null);

        Field mapF = NotificationSubscriptionDAO.class.getDeclaredField("keyToObject");
        mapF.setAccessible(true);
        Map keyMap = (Map) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> deviceMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> sessionMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());

        Class<?> c = NotificationSubscriptionDAO.class.getClassLoader().loadClass(
                "com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO$Key");
        Constructor lego = c.getConstructor(NotificationSubscriptionDAO.class, Long.class, String.class);
        lego.setAccessible(true);
        Object key = lego.newInstance(dataSource.notificationSubscriptions(), (Long) null, sessionId);

        assertTrue(keyMap.keySet().contains(key));

        /*--------------*/

        Set set = (Set) sessionMap.get(sessionId);
        assertNotNull(set);
        assertEquals(1, set.size());

        /*--------------*/

        set = (Set) deviceMap.get(null);
        assertNotNull(set);
        assertEquals(1, set.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveNotificationsSubscription() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        List<Long> ids = new ArrayList<>();
        ids.add(Long.valueOf(1L));
        ids.add(Long.valueOf(2L));
        ids.add(Long.valueOf(3L));

        dataSource.addNotificationsSubscription(sessionId, ids);

        Field mapF = NotificationSubscriptionDAO.class.getDeclaredField("keyToObject");
        mapF.setAccessible(true);
        Map keyMap = (Map) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> deviceMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> sessionMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());

        Class<?> c = NotificationSubscriptionDAO.class.getClassLoader().loadClass(
                "com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO$Key");
        Constructor lego = c.getConstructor(NotificationSubscriptionDAO.class, Long.class, String.class);
        lego.setAccessible(true);
        Object key = lego.newInstance(dataSource.notificationSubscriptions(), ids.get(0), sessionId);

        dataSource.removeNotificationSubscriptions(sessionId, Collections.<Long>emptyList());
        assertTrue(keyMap.keySet().contains(key));

        Set set = (Set) sessionMap.get(sessionId);
        assertNotNull(set);
        assertEquals(ids.size(), set.size());

        set = (Set) deviceMap.get(ids.get(0));
        assertNotNull(set);
        assertEquals(1, set.size());

        set = (Set) deviceMap.get(ids.get(1));
        assertNotNull(set);
        assertEquals(1, set.size());

        /*--------------*/
        
        dataSource.removeNotificationSubscriptions(sessionId, ids);
        assertFalse(keyMap.keySet().contains(key));

        set = (Set) sessionMap.get(sessionId);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set = (Set) deviceMap.get(ids.get(0));
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set = (Set) deviceMap.get(ids.get(1));
        assertNotNull(set);
        assertTrue(set.isEmpty());        
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveNotificationSubscription() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        List<Long> ids = new ArrayList<>();
        ids.add(Long.valueOf(1L));
        ids.add(Long.valueOf(2L));
        ids.add(Long.valueOf(3L));

        dataSource.addNotificationsSubscription(sessionId, ids);

        Field mapF = NotificationSubscriptionDAO.class.getDeclaredField("keyToObject");
        mapF.setAccessible(true);
        Map keyMap = (Map) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("deviceToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> deviceMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());
        mapF = NotificationSubscriptionDAO.class.getDeclaredField("sessionToObject");
        mapF.setAccessible(true);
        Map<Long, NotificationsSubscription> sessionMap = (Map<Long, NotificationsSubscription>) mapF.get(dataSource.notificationSubscriptions());

        Class<?> c = NotificationSubscriptionDAO.class.getClassLoader().loadClass(
                "com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO$Key");
        Constructor lego = c.getConstructor(NotificationSubscriptionDAO.class, Long.class, String.class);
        lego.setAccessible(true);
        Object key = lego.newInstance(dataSource.notificationSubscriptions(), ids.get(0), sessionId);

        /*--------------*/
        
        dataSource.removeNotificationSubscription(ids.get(1));
        assertTrue(keyMap.keySet().contains(key));
        dataSource.removeNotificationSubscription(ids.get(0));
        assertFalse(keyMap.keySet().contains(key));

        /*--------------*/
        
        Set set = (Set) sessionMap.get(sessionId);
        assertNotNull(set);
        assertEquals(1, set.size());

        set = (Set) deviceMap.get(ids.get(0));
        assertNull(set);

        set = (Set) deviceMap.get(ids.get(2));
        assertNotNull(set);
        assertEquals(1, set.size());
    }

    @Test
    public void testCommandSubscriptions() {
        assertNotNull(dataSource.commandSubscriptions());
    }

    @Test
    public void testCommandUpdatesSubscriptions() {
        assertNotNull(dataSource.commandUpdatesSubscriptions());
    }

    @Test
    public void testNotificationSubscriptions() {
        assertNotNull(dataSource.notificationSubscriptions());
    }

}
