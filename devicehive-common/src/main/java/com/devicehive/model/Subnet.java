package com.devicehive.model;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.exceptions.HiveException;
import org.apache.commons.net.util.SubnetUtils;

import javax.ws.rs.core.Response;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Subnet {

    //        private static final int IPv6_MASK_MAX_VALUE = 64;
    private static final int IPv4_MASK_MAX_VALUE = 32;
    private InetAddress inetAddress;
    private int mask;
    private String subnet;

    public Subnet() {
    }

    public Subnet(String subnet) {
        String[] parts = subnet.split("/");
        try {

            this.inetAddress = InetAddress.getByName(parts[0]);
            if (parts.length == 1) {
                mask = IPv4_MASK_MAX_VALUE;
                this.subnet = subnet + "/32";
            } else {
                this.mask = Integer.parseInt(parts[1]);
                this.subnet = subnet;
            }
            if (inetAddress instanceof Inet4Address && mask > IPv4_MASK_MAX_VALUE) {
                throw new IllegalArgumentException("Invalid mask value : " + mask);
            }
        } catch (UnknownHostException e) {
            throw new HiveException("Unable to resolve subnet", Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    public boolean isAddressFromSubnet(InetAddress ip) {
        if (ip instanceof Inet6Address) {
            return false;
        }
        if (inetAddress.getHostAddress().equalsIgnoreCase(ip.getHostAddress()) && mask == 32) {
            return true;
        }
        if (mask == 0) {
            return true;
        }
        SubnetUtils utils = new SubnetUtils(subnet);
        return utils.getInfo().isInRange(ip.getHostAddress());
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public int getMask() {
        return mask;
    }

    public String getSubnet() {
        return subnet;
    }

}
