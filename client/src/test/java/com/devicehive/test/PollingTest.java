package com.devicehive.test;

import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.exceptions.HiveException;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;

@RunWith(JUnit4.class)
public class PollingTest {

    @Test
    public void dummyTest(){
        TestCase.assertEquals(true, true);
    }


    @Test
    public void commandsPollingTest() {
        HiveClient client= null;
        try {
            client = HiveFactory.createClient(URI.create("http://localhost:8080/hive/rest"), false, null, null);
            client.getInfo();
        } catch (HiveException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}
