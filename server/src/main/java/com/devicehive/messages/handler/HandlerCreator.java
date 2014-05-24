package com.devicehive.messages.handler;

import com.google.gson.JsonObject;

public interface HandlerCreator {

    Runnable getHandler(JsonObject message);
}
