package com.devicehive.domain;

/**
 * Created by tmatvienko on 2/5/15.
 */
public class AppInfo {
    private String appVersion;
    private String serverDate;

    public AppInfo(String appVersion, String serverDate) {
        this.appVersion = appVersion;
        this.serverDate = serverDate;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getServerDate() {
        return serverDate;
    }

    public void setServerDate(String serverDate) {
        this.serverDate = serverDate;
    }
}
