package com.devicehive.controller;

import com.devicehive.connect.ClusterConfiguration;
import com.devicehive.domain.AppInfo;
import com.devicehive.domain.ClusterConfig;
import com.devicehive.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tmatvienko on 2/5/15.
 */
@RestController
@RequestMapping("/info")
public class AppInfoController {
    private static final AppInfo APP_INFO = new AppInfo(Constants.API_VERSION,
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(new Date()));

    @Autowired
    private ClusterConfiguration configuration;

    @RequestMapping(value = "/version", method = RequestMethod.GET, headers = "Accept=application/json")
    public AppInfo getAppInfo() {
        return APP_INFO;
    }

    @RequestMapping(value = "/cluster", method = RequestMethod.GET, headers = "Accept=application/json")
    public ClusterConfig getClusterConfig() {
        return configuration.getClusterConfig();
    }
}