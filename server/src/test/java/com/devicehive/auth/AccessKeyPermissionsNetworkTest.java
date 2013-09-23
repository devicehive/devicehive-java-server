package com.devicehive.auth;

import com.devicehive.Constants;
import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.model.*;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AccessKeyPermissionsNetworkTest {

    private AccessKey key = new AccessKey();
    private AccessKeyService accessKeyService = new AccessKeyService();
    private UserService userService;

    private static final User CLIENT = new User() {{
        setId(Constants.ACTIVE_CLIENT_ID);
        setLogin("client");
        setRole(UserRole.CLIENT);
        setStatus(UserStatus.ACTIVE);
    }};

    @Before
    public void initAccessKeyService() {
        userService = mock(UserService.class);
        accessKeyService.setUserService(userService);
    }

    @Test
    public void networkCleanPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setNetworkIds(new JsonStringWrapper("[\"1\",\"2\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setNetworkIds(new JsonStringWrapper("[]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setNetworkIds(null);
        permissions.add(permission3);

        boolean result = CheckPermissionsHelper.checkNetworks(permissions);
        assertTrue(result);
        assertEquals(2, permissions.size());
    }

    @Test
    public void networkEmptyPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();
        boolean result = CheckPermissionsHelper.checkNetworks(permissions);
        assertFalse(result);
    }


    @Test
    public void hasAccessToNetworkOnePermissionSuccessTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        singlePermission.setNetworkIds(new JsonStringWrapper("[42]"));
        permissions.add(singlePermission);

        Set<Network> networks = new HashSet<>();
        Network network1 = new Network();
        network1.setId(0L);
        Network network2 = new Network();
        network2.setId(102L);
        Network network3 = new Network();
        network3.setId(42L);
        networks.add(network1);
        networks.add(network2);
        networks.add(network3);

        CLIENT.setNetworks(networks);
        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.findUserWithNetworks(Constants.ACTIVE_CLIENT_ID)).thenReturn(CLIENT);

        boolean result = accessKeyService.hasAccessToNetwork(key, network3);
        assertTrue(result);
        assertEquals(1, permissions.size());
    }

    @Test
    public void hasAccessToNetworkOnePermissionDeniedTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        singlePermission.setNetworkIds(new JsonStringWrapper("[103]"));
        permissions.add(singlePermission);

        Set<Network> networks = new HashSet<>();
        Network network1 = new Network();
        network1.setId(0L);
        Network network2 = new Network();
        network2.setId(102L);
        Network network3 = new Network();
        network3.setId(42L);
        networks.add(network1);
        networks.add(network2);
        networks.add(network3);

        CLIENT.setNetworks(networks);
        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.findUserWithNetworks(Constants.ACTIVE_CLIENT_ID)).thenReturn(CLIENT);

        boolean result = accessKeyService.hasAccessToNetwork(key, network3);
        assertFalse(result);
        assertEquals(0, permissions.size());
    }

    @Test
    public void hasAccessToNetworkSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setNetworkIds(new JsonStringWrapper("[42]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setNetworkIds(new JsonStringWrapper("[\"128\", \"102\", \"456678\"]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setNetworkIds(new JsonStringWrapper("[\"42\", \"42\", \"0\"]"));
        permissions.add(permission3);

        AccessKeyPermission permission4 = new AccessKeyPermission();
        permission4.setNetworkIds(new JsonStringWrapper("[\"0\"]"));
        permissions.add(permission4);

        Set<Network> networks = new HashSet<>();
        Network network1 = new Network();
        network1.setId(0L);
        Network network2 = new Network();
        network2.setId(102L);
        Network network3 = new Network();
        network3.setId(42L);
        networks.add(network1);
        networks.add(network2);
        networks.add(network3);

        CLIENT.setNetworks(networks);
        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.findUserWithNetworks(Constants.ACTIVE_CLIENT_ID)).thenReturn(CLIENT);

        boolean result = accessKeyService.hasAccessToNetwork(key, network3);
        assertTrue(result);
        assertEquals(2, permissions.size());
    }

    @Test
    public void hasNoAccessToNetworkSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setNetworkIds(new JsonStringWrapper("[33]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setNetworkIds(new JsonStringWrapper("[\"128\", \"102\", \"456678\"]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setNetworkIds(new JsonStringWrapper("[\"102\", \"102\", \"0\"]"));
        permissions.add(permission3);

        AccessKeyPermission permission4 = new AccessKeyPermission();
        permission4.setNetworkIds(new JsonStringWrapper("[\"0\"]"));
        permissions.add(permission4);

        Set<Network> networks = new HashSet<>();
        Network network1 = new Network();
        network1.setId(0L);
        Network network2 = new Network();
        network2.setId(102L);
        Network network3 = new Network();
        network3.setId(42L);
        networks.add(network1);
        networks.add(network2);
        networks.add(network3);

        CLIENT.setNetworks(networks);
        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.findUserWithNetworks(Constants.ACTIVE_CLIENT_ID)).thenReturn(CLIENT);

        boolean result = accessKeyService.hasAccessToNetwork(key, network3);
        assertFalse(result);
        assertEquals(0, permissions.size());
    }


}
