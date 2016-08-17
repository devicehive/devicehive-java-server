package com.devicehive.controller;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

    @RequestMapping("/swagger")
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
