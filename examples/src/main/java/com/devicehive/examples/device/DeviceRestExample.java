package com.devicehive.examples.device;


import com.devicehive.client.model.Role;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.examples.Example;

import java.io.PrintStream;
import java.net.URISyntaxException;

public class DeviceRestExample extends Example{

    protected DeviceRestExample(PrintStream err, PrintStream out, String... args) throws HiveException, URISyntaxException {
        super(err, out, Role.DEVICE, args);
    }
    @Override
    public void addExtraOptions(){
//        options.addOption()
    }

    @Override
    public void run() {

    }
}
