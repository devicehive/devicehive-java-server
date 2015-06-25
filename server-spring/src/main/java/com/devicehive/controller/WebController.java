package com.devicehive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    @RequestMapping("/oauth2")
    public String oauth2() {
        return "templates/oauth2/index";
    }

    @RequestMapping("/login")
    public String oauth2Login() {
        return "templates/oauthLogin/login";
    }

    @RequestMapping("/home")
    public String oauth2Home() {
        return "templates/oauthLogin/home";
    }

}
