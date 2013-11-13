package com.devicehive.client.example.user;


import com.devicehive.client.api.*;
import com.devicehive.client.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TODO
 */
public class UserRestExample extends UserExample {

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        UserRestExample example = new UserRestExample();
        URI rest = URI.create(args[0]);
        URI websocket = URI.create(args[1]);
        example.run(rest, websocket, Transport.REST_ONLY);
    }


}
