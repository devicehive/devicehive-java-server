package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.service.configuration.ConfigurationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ConfigurationServiceTest extends AbstractResourceTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void should_save_configuration_property_and_return_by_name() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));
    }

    @Test
    public void should_update_config_property() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));

        String newVal = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, newVal);

        savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(newVal));
    }

    @Test
    public void should_delete_config_property() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));

        configurationService.delete(key);

        savedVal = configurationService.get(key);
        assertThat(savedVal, nullValue());
    }
}
