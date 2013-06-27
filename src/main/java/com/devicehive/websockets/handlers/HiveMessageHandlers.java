package com.devicehive.websockets.handlers;


import com.devicehive.exceptions.HiveException;
import com.google.gson.JsonObject;

import javax.websocket.Session;

public interface HiveMessageHandlers {

    void ensureAuthorised(JsonObject request, Session session) throws HiveException;
}
