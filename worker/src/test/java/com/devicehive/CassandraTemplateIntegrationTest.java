package com.devicehive;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.domain.DeviceNotification;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

;

/**
 * Created by tmatvienko on 2/2/15.
 */
public class CassandraTemplateIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CassandraOperations cassandraTemplate;

    final private Timestamp date = new Timestamp(System.currentTimeMillis());
    final private String deviceGuid = UUID.randomUUID().toString();

    @Test
    public void supportsPojoToCqlMappings() {
        final DeviceNotification notif = new DeviceNotification(String.valueOf(UUIDs.timeBased().timestamp()), deviceGuid, date, "notification1", null);
        cassandraTemplate.insert(notif);

        Select select = QueryBuilder.select().from("device_notification").where(QueryBuilder.eq("device_guid", deviceGuid)).limit(10);

        DeviceNotification retrievedNotification = cassandraTemplate.selectOne(select, DeviceNotification.class);
        assertThat(retrievedNotification, IsEqual.equalTo(notif));

        List<DeviceNotification> retrievedNotifications = cassandraTemplate.select(select, DeviceNotification.class);

        assertThat(retrievedNotifications.size(), is(1));
        assertThat(retrievedNotifications, hasItem(notif));
    }
}
