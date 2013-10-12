package com.devicehive.auth;


import com.devicehive.model.*;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public class AccessKeyPermissionDomainsTest {

    @Test
    public void domainsCleanPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();

        ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1
                .setDomains(new JsonStringWrapper("[\"http://test.test.com\",\".net\",\".devicehive.com\"]"));

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDomains(new JsonStringWrapper("[]"));

        AccessKeyPermission permission3 = new AccessKeyPermission();

        permissions.add(permission1);
        permissions.add(permission2);
        permissions.add(permission3);

        boolean result = CheckPermissionsHelper.checkDomains(permissions);
        assertTrue(result);
        assertEquals(2, permissions.size());
        ThreadLocalVariablesKeeper.clean();
    }

    @Test
    public void domainsEmptyPermissionsCase() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
        boolean result = CheckPermissionsHelper.checkDomains(permissions);
        assertFalse(result);
        ThreadLocalVariablesKeeper.clean();
    }

    @Test
    public void hasNoAccessToDomainOnePermissionSuccessTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");
        singlePermission.setDomains(new JsonStringWrapper("[\".net\", \".amm.ru\"]"));
        permissions.add(singlePermission);

        boolean result = CheckPermissionsHelper.checkDomains(permissions);
        assertFalse(result);
        assertEquals(0, permissions.size());
        ThreadLocalVariablesKeeper.clean();
    }

    @Test
    public void hasAccessToDomainSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();
        ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setDomains(new JsonStringWrapper("[\".net\", \".amm.ru\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDomains(new JsonStringWrapper("[]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setDomains(new JsonStringWrapper("[\".net\", \".com\"]"));
        permissions.add(permission3);

        AccessKeyPermission permission4 = new AccessKeyPermission();
        permission4.setDomains(new JsonStringWrapper("[\".devicehive.com\"]"));
        permissions.add(permission4);

        AccessKeyPermission permission5 = new AccessKeyPermission();
        permission5.setDomains(new JsonStringWrapper("[\".devicehive.\"]"));
        permissions.add(permission5);

        boolean result = CheckPermissionsHelper.checkDomains(permissions);
        assertTrue(result);
        assertEquals(2, permissions.size());
        ThreadLocalVariablesKeeper.clean();
    }

    @Test
    public void hasNoAccessToDomainSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();
        ThreadLocalVariablesKeeper.setHostName("http://test.devicehive.com");

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setDomains(new JsonStringWrapper("[\".net\", \".amm.ru\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDomains(new JsonStringWrapper("[]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setDomains(new JsonStringWrapper("[\".devicehive.\"]"));
        permissions.add(permission3);

        boolean result = CheckPermissionsHelper.checkDomains(permissions);
        assertFalse(result);
        assertEquals(0, permissions.size());
        ThreadLocalVariablesKeeper.clean();
    }
}
