package com.devicehive.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/hello")
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        logger.info("Hello controller called");
        return "Hi there!";
    }

}
