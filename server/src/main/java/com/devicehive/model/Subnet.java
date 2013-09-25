package com.devicehive.model;


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
            if (parts.length == 1){
              mask = IPv4_MASK_MAX_VALUE;
                this.subnet = subnet + "/32";
            } else{
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
            //TODO support for IPv6
            return false;
        }
        if (inetAddress.getHostAddress().equalsIgnoreCase(ip.getHostAddress()) && mask == 32){
            return true;
        }
        if (mask == 0){
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

    public String getSubnet(){
        return subnet;
    }

}
