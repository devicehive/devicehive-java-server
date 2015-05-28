package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class HelloControllerTest extends AbstractResourceTest {

    @Test
    public void testHello() throws Exception {
        ResponseEntity<String> resp = template.getForEntity(baseUri + "/hello", String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertEquals("Hi there!", resp.getBody());
    }
}
