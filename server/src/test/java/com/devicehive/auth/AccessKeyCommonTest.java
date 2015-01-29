package com.devicehive.auth;

import com.devicehive.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class AccessKeyCommonTest {

    private static User CLIENT = new User() {
        {
            setId(Constants.ACTIVE_CLIENT_ID);
            setLogin("client");
            setRole(UserRole.CLIENT);
            setStatus(UserStatus.ACTIVE);
        }

        private static final long serialVersionUID = 2596203145671715890L;
    };
    private static User ADMIN = new User() {
        {
            setId(Constants.ACTIVE_ADMIN_ID);
            setLogin("admin");
            setRole(UserRole.ADMIN);
            setStatus(UserStatus.ACTIVE);
        }

        private static final long serialVersionUID = -5580164266287051549L;
    };

    @Test
    public void userStatusTest() throws Exception {
        AccessKey key = new AccessKey();
        ADMIN.setStatus(UserStatus.DISABLED);
        key.setUser(ADMIN);
        HiveSecurityContext hiveSecurityContext = new HiveSecurityContext();
        hiveSecurityContext.setHivePrincipal(new HivePrincipal(null, null, key));
        RequestInterceptor interceptor = new RequestInterceptor();
        interceptor.setHiveSecurityContext(hiveSecurityContext);
        Exception thrown = null;
        try {
            interceptor.checkPermissions(null);
        } catch (Exception e) {
            thrown = e;
            if (e instanceof HiveException) {
                HiveException hiveException = (HiveException) e;
                assertEquals(401, hiveException.getCode().intValue());
            } else {
                fail("Hive exception expected");
            }
        }
        if (thrown == null) {
            fail("Hive exception expected");
        }

        key = new AccessKey();
        ADMIN.setStatus(UserStatus.LOCKED_OUT);
        key.setUser(ADMIN);
        hiveSecurityContext = new HiveSecurityContext();
        hiveSecurityContext.setHivePrincipal(new HivePrincipal(null, null, key));
        interceptor = new RequestInterceptor();
        interceptor.setHiveSecurityContext(hiveSecurityContext);
        thrown = null;
        try {
            interceptor.checkPermissions(null);
        } catch (Exception e) {
            thrown = e;
            if (e instanceof HiveException) {
                HiveException hiveException = (HiveException) e;
                assertEquals(401, hiveException.getCode().intValue());
            } else {
                fail("Hive exception expected");
            }
        }
        if (thrown == null) {
            fail("Hive exception expected");
        }

        key = new AccessKey();
        ADMIN.setStatus(UserStatus.DELETED);
        key.setUser(ADMIN);
        hiveSecurityContext = new HiveSecurityContext();
        hiveSecurityContext.setHivePrincipal(new HivePrincipal(null, null, key));
        interceptor = new RequestInterceptor();
        interceptor.setHiveSecurityContext(hiveSecurityContext);
        thrown = null;
        try {
            interceptor.checkPermissions(null);
        } catch (Exception e) {
            thrown = e;
            if (e instanceof HiveException) {
                HiveException hiveException = (HiveException) e;
                assertEquals(401, hiveException.getCode().intValue());
            } else {
                fail("Hive exception expected");
            }
        }
        if (thrown == null) {
            fail("Hive exception expected");
        }
    }

    @Test
    public void expirationDateTest() {
        AccessKey key = new AccessKey();
        CLIENT.setStatus(UserStatus.ACTIVE);
        key.setUser(CLIENT);
        Timestamp inPast = new Timestamp(0);
        key.setExpirationDate(inPast);
        HiveSecurityContext hiveSecurityContext = new HiveSecurityContext();
        hiveSecurityContext.setHivePrincipal(new HivePrincipal(null, null, key));
        RequestInterceptor interceptor = new RequestInterceptor();
        interceptor.setHiveSecurityContext(hiveSecurityContext);
        try {
            interceptor.checkPermissions(null);
        } catch (Exception e) {
            if (e instanceof HiveException) {
                HiveException hiveException = (HiveException) e;
                assertEquals(401, hiveException.getCode().intValue());
            } else {
                fail("Hive exception expected");
            }
        }
    }

}
