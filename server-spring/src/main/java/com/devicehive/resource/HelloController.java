package com.devicehive.resource;

import com.devicehive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Service
@Singleton
@Path("/hello")
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private UserService userService;

    @GET
    @PreAuthorize("hasAuthority('ADMIN')")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        logger.info("Hello controller called");
        logger.info(this.toString());
        return "Hi there!";
    }

}
