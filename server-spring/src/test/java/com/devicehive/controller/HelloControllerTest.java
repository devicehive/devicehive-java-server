package com.devicehive.controller;

import com.devicehive.base.AbstractResourceTest;
import org.junit.Assert;
import org.junit.Test;

public class HelloControllerTest extends AbstractResourceTest {

    @Test
    public void testHello() throws Exception {
        String resp = target().path("hello").request().get(String.class);
        Assert.assertEquals("Hi there!", resp);
    }
}
