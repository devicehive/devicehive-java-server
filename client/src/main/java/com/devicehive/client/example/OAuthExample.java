package com.devicehive.client.example;


import com.devicehive.client.api.*;
import com.devicehive.client.api.client.*;
import com.devicehive.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.UUID;

public class OAuthExample {

    private static Logger logger = LoggerFactory.getLogger(OAuthExample.class);
    private Client client;
    private PrintStream out;


    public OAuthExample(PrintStream out) {
        this.out = out;
    }

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     *             args[2] - the domain of the current application
     */
    public static void main(String... args) {
        OAuthExample example = new OAuthExample(System.out);
        if (args.length < 3) {
            example.printUsage();
        } else {
            URI rest = URI.create(args[0]);
            URI websocket = URI.create(args[1]);
            try {
                example.init(rest, websocket);
                System.out.println();
                example.token(args[2], rest, websocket);
            } finally {
                example.close();
            }
        }
    }

    private void init(URI rest, URI websocket) {
        client = new Client(rest, websocket, Transport.REST_ONLY);
        client.authenticate("dhadmin", "dhadmin_#911");
    }

    private void close() {
        try {
            client.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void token(String domain, URI rest, URI websocket) {
        //firstly create OAuthClient
        OAuthClient oauthClient = new OAuthClient();
        oauthClient.setName("example client");
        oauthClient.setRedirectUri("http://www.devicehive.com/");
        oauthClient.setDomain(domain);
        oauthClient.setOauthId("example" + UUID.randomUUID().toString().substring(15));
        OAuthClientController clientController = client.getOAuthClientController();
        clientController.insert(oauthClient);
        // then, create a grant
        OAuthGrantController grantController = client.getOAuthGrantController();
        OAuthGrant grant = new OAuthGrant();
        grant.setClient(oauthClient);
        grant.setType(OAuthType.CODE);
        grant.setAccessType(AccessType.OFFLINE);
        grant.setRedirectUri("http://www.devicehive.com/");
        grant.setScope(AllowedAction.GET_DEVICE.getValue());
        Long userId = 1L;
        OAuthGrant newGrant = grantController.insert(userId, grant);
        //exchange code
        OAuthTokenController tokenController = client.getOAuthTokenController();
        AccessToken token = tokenController.requestAccessToken("authorization_code", newGrant.getAuthCode(),
                grant.getRedirectUri(), grant.getClient().getOauthId(), null, null, null);
        //finally, try to use access token
        String accessToken = token.getAccessToken();
        try (Client accessTokenClient = new Client(rest, websocket, Transport.REST_ONLY)) {
            accessTokenClient.authenticate(accessToken);
            DeviceController controller = accessTokenClient.getDeviceController();
            String guid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase();
            Device device = controller.getDevice(guid);
            out.print("device received. Device name: " + device.getName());
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void printUsage() {
        out.println("Params missed!");
        out.println("1'st param - REST URL");
        out.println("2'nd param - websocket URL");
        out.println("3'rd param - domain of the current application");
    }

}
