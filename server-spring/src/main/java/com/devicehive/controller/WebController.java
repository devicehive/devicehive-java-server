package com.devicehive.controller;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private Environment env;

    @RequestMapping("/config")
    public String index(Model model) {
        model.addAttribute("restUrl", configurationService.get(Constants.REST_SERVER_URL));
        model.addAttribute("wsUrl", configurationService.get(Constants.WEBSOCKET_SERVER_URL));
        model.addAttribute("buildVersion", env.getProperty("build.version"));
        model.addAttribute("buildTimestamp", env.getProperty("build.timestamp"));
        return "server/templates/index";
    }

    @RequestMapping("/oauth2")
    public String oauth2() {
        return "server/templates/oauth2/index";
    }

    @RequestMapping("/login")
    public String oauth2Login() {
        return "server/templates/oauthLogin/login";
    }

    @RequestMapping("/home")
    public String oauth2Home() {
        return "server/templates/oauthLogin/home";
    }

    @RequestMapping({"", "/admin"})
    public String adminConsole() {
        return "index";
    }
}
