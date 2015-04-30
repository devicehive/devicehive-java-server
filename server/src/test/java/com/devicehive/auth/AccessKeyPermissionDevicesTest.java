package com.devicehive.auth;

import com.devicehive.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Device;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AccessKeyPermissionDevicesTest {

    private static final User CLIENT = new User() {
        {
            setId(Constants.ACTIVE_CLIENT_ID);
            setLogin("client");
            setRole(UserRole.CLIENT);
            setStatus(UserStatus.ACTIVE);
        }

        private static final long serialVersionUID = 1330575406466966753L;
    };
    private AccessKey key = new AccessKey();
    @InjectMocks
    private AccessKeyService accessKeyService = new AccessKeyService();
    @Mock
    private UserService userService;

    @Before
    public void initAccessKeyService() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void deviceCleanPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1
            .setDeviceGuids(new JsonStringWrapper("[\"" + UUID.randomUUID() + "\",\"" + UUID.randomUUID() + "\"]"));

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDeviceGuids(new JsonStringWrapper("[]"));

        AccessKeyPermission permission3 = new AccessKeyPermission();

        permissions.add(permission1);
        permissions.add(permission2);
        permissions.add(permission3);

        CheckPermissionsHelper.filterDeviceGuids(permissions);
        assertEquals(2, permissions.size());
    }

    @Test
    public void deviceEmptyPermissionsCase() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        CheckPermissionsHelper.filterDeviceGuids(permissions);
        assertEquals(0, permissions.size());
    }

    @Test
    @Ignore
    public void hasAccessToDeviceOnePermissionSuccessTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        UUID usefulGuid = UUID.randomUUID();
        singlePermission.setDeviceGuids(
            new JsonStringWrapper("[\"" + usefulGuid + "\", \"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(singlePermission);

        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.hasAccessToDevice(any(User.class), any(String.class))).thenReturn(true);

        Device device = new Device();
        device.setGuid(usefulGuid.toString());

        boolean result = accessKeyService.hasAccessToDevice(key, device.getGuid());
        Assert.assertTrue(result);
        Assert.assertEquals(1, permissions.size());
    }

    //   TODO @Test
    public void hasNoAccessToDeviceOnePermissionTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        UUID usefulGuid = UUID.randomUUID();
        singlePermission.setDeviceGuids(
            new JsonStringWrapper(
                "[\"" + UUID.randomUUID().toString() + "\", \"" + UUID.randomUUID().toString() + "\", " +
                "\"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(singlePermission);

        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.hasAccessToDevice(any(User.class), any(String.class))).thenReturn(true);

        Device device = new Device();
        device.setGuid(usefulGuid.toString());

        boolean result = accessKeyService.hasAccessToDevice(key, device.getGuid());
        Assert.assertFalse(result);
        Assert.assertEquals(0, permissions.size());
    }

    //   TODO @Test
    public void hasAccessToDeviceSeveralPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        UUID usefulGuid = UUID.randomUUID();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setDeviceGuids(
            new JsonStringWrapper("[\"" + usefulGuid + "\", \"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDeviceGuids(
            new JsonStringWrapper("[\"" + UUID.randomUUID() + "\", \"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permissions.add(permission3);

        AccessKeyPermission permission4 = new AccessKeyPermission();
        permission4.setDeviceGuids(new JsonStringWrapper("[\"" + UUID.randomUUID() + "\", " +
                                                         "\"" + UUID.randomUUID().toString() + "\", \"" + usefulGuid
            .toString() + "\"]"));
        permissions.add(permission4);

        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.hasAccessToDevice(any(User.class), any(String.class))).thenReturn(true);

        Device device = new Device();
        device.setGuid(usefulGuid.toString());

        boolean result = accessKeyService.hasAccessToDevice(key, device.getGuid());
        Assert.assertTrue(result);
        Assert.assertEquals(3, permissions.size());
    }

    //  TODO  @Test
    public void hasNoAccessToDeviceSeveralPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setDeviceGuids(
            new JsonStringWrapper(
                "[\"" + UUID.randomUUID().toString() + "\", \"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDeviceGuids(
            new JsonStringWrapper("[\"" + UUID.randomUUID() + "\", \"" + UUID.randomUUID().toString() + "\"]"));
        permissions.add(permission2);

        key.setUser(CLIENT);
        key.setPermissions(permissions);

        when(userService.hasAccessToDevice(any(User.class), any(String.class))).thenReturn(true);

        UUID usefulGuid = UUID.randomUUID();
        Device device = new Device();
        device.setGuid(usefulGuid.toString());

        boolean result = accessKeyService.hasAccessToDevice(key, device.getGuid());
        Assert.assertFalse(result);
        Assert.assertEquals(0, permissions.size());
    }

}
