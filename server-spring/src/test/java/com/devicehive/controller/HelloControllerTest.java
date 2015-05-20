package com.devicehive.controller;

import com.devicehive.base.AbstractResourceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HelloControllerTest extends AbstractResourceTest {

    @Test
    public void testHello() throws Exception {
        ResponseEntity<String> resp = template.getForEntity(baseUri + "/hello", String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertEquals("Hi there!", resp.getBody());
    }
}
