package com.devicehive.auth;


import com.devicehive.model.*;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
@SuppressWarnings({"serialization"})
public class AccessKeyPermissionDomainsTest {

    @Test
    public void domainsCleanPermissionsTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();


        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1
                .setDomains(new JsonStringWrapper("[\"http://test.test.com\",\".net\",\".devicehive.com\"]"));

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDomains(new JsonStringWrapper("[]"));

        AccessKeyPermission permission3 = new AccessKeyPermission();

        permissions.add(permission1);
        permissions.add(permission2);
        permissions.add(permission3);

        CheckPermissionsHelper.filterDomains("test.devicehive.com", permissions);
        assertEquals(2, permissions.size());
    }

    @Test
    public void domainsEmptyPermissionsCase() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        CheckPermissionsHelper.filterDomains("test.devicehive.com", permissions);
        assertEquals(0, permissions.size());
    }

    @Test
    public void hasNoAccessToDomainOnePermissionSuccessTest() {
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission singlePermission = new AccessKeyPermission();
        singlePermission.setDomains(new JsonStringWrapper("[\".net\", \".amm.ru\"]"));
        permissions.add(singlePermission);

        CheckPermissionsHelper.filterDomains("test.devicehive.com", permissions);
        assertEquals(0, permissions.size());
    }

    @Test
    public void hasAccessToDomainSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();

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

        CheckPermissionsHelper.filterDomains("test.devicehive.com", permissions);
        assertEquals(2, permissions.size());
    }

    @Test
    public void hasNoAccessToDomainSeveralPermissionsTest(){
        Set<AccessKeyPermission> permissions = new HashSet<>();

        AccessKeyPermission permission1 = new AccessKeyPermission();
        permission1.setDomains(new JsonStringWrapper("[\".net\", \".amm.ru\"]"));
        permissions.add(permission1);

        AccessKeyPermission permission2 = new AccessKeyPermission();
        permission2.setDomains(new JsonStringWrapper("[]"));
        permissions.add(permission2);

        AccessKeyPermission permission3 = new AccessKeyPermission();
        permission3.setDomains(new JsonStringWrapper("[\".devicehive.\"]"));
        permissions.add(permission3);

        CheckPermissionsHelper.filterDomains("test.devicehive.com", permissions);
        assertEquals(0, permissions.size());
        ThreadLocalVariablesKeeper.clean();
    }
}
