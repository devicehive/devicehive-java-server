package com.devicehive.messages.handler;

import com.google.gson.JsonObject;

public interface ClientHandler {

    void sendMessage(JsonObject json);

}
