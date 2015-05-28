package com.devicehive.controller;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private Environment env;

    @RequestMapping("/greeting")
    public String greering(@RequestParam(value = "name", required = false, defaultValue = "anon") String name, Model model) {
        model.addAttribute("somename", name);
        return "greeting";
    }

    @RequestMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("restUrl", configurationService.get(Constants.REST_SERVER_URL));
        model.addAttribute("wsUrl", configurationService.get(Constants.WEBSOCKET_SERVER_URL));
        model.addAttribute("buildVersion", env.getProperty("build.version"));
        model.addAttribute("buildTimestamp", env.getProperty("build.timestamp"));
        return "index";
    }

}
