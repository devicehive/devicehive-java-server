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
