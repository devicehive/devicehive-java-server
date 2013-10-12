package com.devicehive.auth;

import com.devicehive.Constants;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.controller.DeviceCommandController;
import com.devicehive.controller.DeviceController;
import com.devicehive.controller.DeviceNotificationController;
import com.devicehive.controller.NetworkController;
import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AccessKeyIntegrationTest {

    private static final User ADMIN = new User() {{
        setId(Constants.ACTIVE_ADMIN_ID);
        setLogin("admin");
        setRole(UserRole.ADMIN);
        setStatus(UserStatus.ACTIVE);
    }};
    private static final User CLIENT = new User() {{
        setId(Constants.ACTIVE_CLIENT_ID);
        setLogin("client");
        setRole(UserRole.CLIENT);
        setStatus(UserStatus.ACTIVE);
    }};
    List<Method> allAvailableMethods;
    private AccessKeyService accessKeyService;
    private UserService userService;
    private AccessKeyDAO accessKeyDAO;
    private DeviceDAO deviceDAO;
    private AccessKeyPermissionDAO permissionDAO;
    private UserDAO userDAO;
    private AccessKeyInterceptor interceptor = new AccessKeyInterceptor();
    private InvocationContext context;
    private int methodCalls;

    @Before
    public void initializeDependencies() {
        accessKeyService = new AccessKeyService();
        accessKeyDAO = mock(AccessKeyDAO.class);
        accessKeyService.setAccessKeyDAO(accessKeyDAO);
        deviceDAO = mock(DeviceDAO.class);
        accessKeyService.setDeviceDAO(deviceDAO);
        permissionDAO = mock(AccessKeyPermissionDAO.class);
        accessKeyService.setPermissionDAO(permissionDAO);
        userService = new UserService();
        accessKeyService.setUserService(userService);
        userService.setConfigurationService(mock(ConfigurationService.class));
        userService.setNetworkDAO(mock(NetworkDAO.class));
        userService.setPasswordService(mock(PasswordProcessor.class));
        userDAO = mock(UserDAO.class);
        userService.setUserDAO(userDAO);
        context = mock(InvocationContext.class);

        Method[] deviceControllerMethods = DeviceController.class.getMethods();
        Method[] notificationControllerMethods = DeviceNotificationController.class.getMethods();
        Method[] commandControllerMethods = DeviceCommandController.class.getMethods();
        Method[] networkControllerMethods = NetworkController.class.getMethods();
        allAvailableMethods = new ArrayList<>();
        allAvailableMethods.addAll(Arrays.asList(deviceControllerMethods));
        allAvailableMethods.addAll(Arrays.asList(notificationControllerMethods));
        allAvailableMethods.addAll(Arrays.asList(commandControllerMethods));
        allAvailableMethods.addAll(Arrays.asList(networkControllerMethods));
        Iterator<Method> iterator = allAvailableMethods.iterator();
        while (iterator.hasNext()) {
            Method currentMethod = iterator.next();
            if (!currentMethod.isAnnotationPresent(AllowedKeyAction.class)) {
                iterator.remove();
            } else {
                methodCalls++;
            }
        }
    }

    @Test
    public void actionsCaseAllowed() {
        /**
         * Only actions field is not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(CLIENT);

        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            try {
                AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
                List<AllowedKeyAction.Action> allowedActions = Arrays.asList(allowedActionAnnotation.action());
                ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
                ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
                try {
                    ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("8.8.8.8"));
                } catch (UnknownHostException e) {
                    fail("Unexpected exception");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE)) {
                    actionTestProcess(accessKey, "[GetDevice,GetNetwork,GetDeviceNotification]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.REGISTER_DEVICE)) {
                    actionTestProcess(accessKey, "[CreateDeviceNotification,RegisterDevice]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.CREATE_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey, "[CreateDeviceCommand,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION)) {
                    actionTestProcess(accessKey,
                            "[CreateDeviceNotification,CreateDeviceCommand,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey, "[CreateDeviceNotification,GetDeviceCommand,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION)) {
                    actionTestProcess(accessKey,
                            "[CreateDeviceNotification,GetDeviceNotification,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.UPDATE_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey,
                            "[CreateDeviceNotification,GetDeviceNotification,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_NETWORK)) {
                    actionTestProcess(accessKey,
                            "[CreateDeviceNotification,GetNetwork,UpdateDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_STATE)) {
                    actionTestProcess(accessKey, "[GetDeviceState]");
                }
            } catch (Exception e) {
                fail("No exceptions expected from interceptor");
            } finally {
                assertNull(ThreadLocalVariablesKeeper.getClientIP());
                assertNull(ThreadLocalVariablesKeeper.getHostName());
                assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                accessKey.getPermissions().clear();
            }
        }

    }

    @Test
    public void actionsCaseNotAllowed() throws Exception {
        /**
         * Only actions field is not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(ADMIN);
        int exceptionsCounter = 0;
        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            List<AllowedKeyAction.Action> allowedActions = Arrays.asList(allowedActionAnnotation.action());
            ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
            ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
            try {
                ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("8.8.8.8"));
            } catch (UnknownHostException e) {
                fail("Unexpected exception");
            }
            try {
                //if some controllers will contain more than 1 action per method, should be changed
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE)) {
                    actionTestProcess(accessKey, "[RegisterDevice]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.REGISTER_DEVICE)) {
                    actionTestProcess(accessKey, "[GetDevice]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_STATE)) {
                    actionTestProcess(accessKey, "[GetDevice]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.CREATE_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey, "[CreateDeviceNotification]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION)) {
                    actionTestProcess(accessKey, "[GetDeviceCommand]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey, "[CreateDeviceNotification]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION)) {
                    actionTestProcess(accessKey, "[CreateDeviceNotification]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.UPDATE_DEVICE_COMMAND)) {
                    actionTestProcess(accessKey, "[GetNetwork]");
                }
                if (allowedActions.contains(AllowedKeyAction.Action.GET_NETWORK)) {
                    actionTestProcess(accessKey,
                            "[CreateDeviceNotification]");
                }
            } catch (HiveException e) {
                if (e.getCode() != Response.Status.UNAUTHORIZED.getStatusCode()) {
                    fail("Unauthorizd code expected");
                }
                exceptionsCounter++;
            } finally {
                assertNull(ThreadLocalVariablesKeeper.getClientIP());
                assertNull(ThreadLocalVariablesKeeper.getHostName());
                assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                accessKey.getPermissions().clear();
            }


        }
        assertEquals(methodCalls, exceptionsCounter);
    }

    private void actionTestProcess(AccessKey accessKey, String actions) throws Exception {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setAccessKey(accessKey);
        permission.setActions(new JsonStringWrapper(actions));
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        interceptor.checkPermissions(context);
    }

    @Test
    public void subnetsCaseAllowed() {
        /**
         * Only subnets field and actions field are not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(CLIENT);
        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            AllowedKeyAction.Action action = allowedActionAnnotation.action()[0];
            try {
                ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
                ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
                try {
                    ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("8.8.8.8"));
                } catch (UnknownHostException e) {
                    fail("Unexpected exception");
                }
                subnetsTestProcess(accessKey, "[" + action.getValue() + "]");
            } catch (Exception e) {
                fail("No exceptions expected from interceptor");
            } finally {
                assertNull(ThreadLocalVariablesKeeper.getClientIP());
                assertNull(ThreadLocalVariablesKeeper.getHostName());
                assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                accessKey.getPermissions().clear();
            }
        }
    }

    @Test
    public void subnetsCaseNotAllowed() {
        /**
         * Only subnets field and actions field are not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(ADMIN);
        int exceptionsCounter = 0;
        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            AllowedKeyAction.Action action = allowedActionAnnotation.action()[0];
            try {
                ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
                ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
                try {
                    ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("192.150.1.1"));
                } catch (UnknownHostException e) {
                    fail("Unexpected exception");
                }
                try {
                    subnetsTestProcess(accessKey, "[" + action.getValue() + "]");
                } catch (HiveException e) {
                    if (e.getCode() != Response.Status.UNAUTHORIZED.getStatusCode()) {
                        fail("Unauthorizd code expected");
                    }
                    exceptionsCounter++;
                } finally {
                    assertNull(ThreadLocalVariablesKeeper.getClientIP());
                    assertNull(ThreadLocalVariablesKeeper.getHostName());
                    assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                    accessKey.getPermissions().clear();
                }
            } catch (Exception e) {
                fail("No exceptions expected from interceptor");
            }
        }
        assertEquals(exceptionsCounter, methodCalls);
    }

    private void subnetsTestProcess(AccessKey accessKey, String action) throws Exception {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setAccessKey(accessKey);
        String subnets = "[\"8.8.0.0/8\", \"192.168.1.1/32\", \"78.7.3.5\"]";
        permission.setSubnets(new JsonStringWrapper(subnets));
        permission.setActions(new JsonStringWrapper(action));
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        interceptor.checkPermissions(context);
    }

    @Test
    public void domainsCaseAllowed() {
        /**
         * Only subnets field and actions field are not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(CLIENT);
        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            AllowedKeyAction.Action action = allowedActionAnnotation.action()[0];
            try {
                ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
                ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
                try {
                    ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("8.8.8.8"));
                } catch (UnknownHostException e) {
                    fail("Unexpected exception");
                }
                domainsTestProcess(accessKey, "[" + action.getValue() + "]");
            } catch (Exception e) {
                fail("No exceptions expected from interceptor");
            } finally {
                assertNull(ThreadLocalVariablesKeeper.getClientIP());
                assertNull(ThreadLocalVariablesKeeper.getHostName());
                assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                accessKey.getPermissions().clear();
            }
        }
    }

    @Test
    public void domainsCaseNotAllowed() {
        /**
         * Only domains field and actions field are not null
         */
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(ADMIN);
        int exceptionsCounter = 0;
        for (Method method : allAvailableMethods) {
            when(context.getMethod()).thenReturn(method);
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            AllowedKeyAction.Action action = allowedActionAnnotation.action()[0];
            try {
                ThreadLocalVariablesKeeper.setPrincipal(new HivePrincipal(null, null, accessKey));
                ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com.dataart.com");
                try {
                    ThreadLocalVariablesKeeper.setClientIP(InetAddress.getByName("192.150.1.1"));
                } catch (UnknownHostException e) {
                    fail("Unexpected exception");
                }
                try {
                    domainsTestProcess(accessKey, "[" + action.getValue() + "]");
                } catch (HiveException e) {
                    if (e.getCode() != Response.Status.UNAUTHORIZED.getStatusCode()) {
                        fail("Unauthorizd code expected");
                    }
                    exceptionsCounter++;
                } finally {
                    assertNull(ThreadLocalVariablesKeeper.getClientIP());
                    assertNull(ThreadLocalVariablesKeeper.getHostName());
                    assertNull(ThreadLocalVariablesKeeper.getPrincipal());
                    accessKey.getPermissions().clear();
                }
            } catch (Exception e) {
                fail("No exceptions expected from interceptor");
            }
        }
        assertEquals(exceptionsCounter, methodCalls);
    }

    private void domainsTestProcess(AccessKey accessKey, String action) throws Exception {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setAccessKey(accessKey);
        String domains = "[\".net\", \"devicehive.com\"]";
        permission.setDomains(new JsonStringWrapper(domains));
        permission.setActions(new JsonStringWrapper(action));
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        interceptor.checkPermissions(context);
    }
}


