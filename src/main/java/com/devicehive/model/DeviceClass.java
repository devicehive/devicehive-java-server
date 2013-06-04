package com.devicehive.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO JavaDoc
 */
@XmlRootElement
public class DeviceClass {
    public Integer id;
    public String name;
    public String version;
    public Boolean isPermanent;
    public Integer offlineTimeout;
    public Object data;

    public DeviceClass() {

    }
}
