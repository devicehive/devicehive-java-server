package com.devicehive.controller;

import com.devicehive.application.JerseyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

@Controller
public class WebController {

    @Value("${server.context-path}")
    private String contextPath;


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

    @RequestMapping({"/", "/swagger"})
    public String swagger(@RequestParam(value = "url", required = false) String url, HttpServletRequest request, RedirectAttributes redirectAttributes) throws MalformedURLException {
        if (url == null || ValueConstants.DEFAULT_NONE.equals(url)) {
            URL requestUrl = new URL(request.getRequestURL().toString());
            String portString = requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
            String swaggerJsonUrl = requestUrl.getProtocol() + "://" + requestUrl.getHost() + portString + contextPath + JerseyConfig.REST_PATH + "/swagger.json";
            redirectAttributes.addAttribute("url", swaggerJsonUrl);
            return "redirect:/swagger";
        } else {
            return "swagger";
        }
    }
}
