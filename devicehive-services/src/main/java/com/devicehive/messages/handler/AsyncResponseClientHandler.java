package com.devicehive.messages.handler;

import com.google.gson.JsonObject;

import javax.ws.rs.container.AsyncResponse;

public class AsyncResponseClientHandler implements ClientHandler {

    private AsyncResponse asyncResponse;

    public AsyncResponseClientHandler(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    @Override
    public void sendMessage(JsonObject json) {
        asyncResponse.resume(json);
    }

}
