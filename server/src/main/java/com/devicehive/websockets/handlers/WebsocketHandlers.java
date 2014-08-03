package com.devicehive.websockets.handlers;

import com.devicehive.auth.Authorized;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.handlers.annotations.WebsocketController;

@Authorized
@LogExecutionTime
@WebsocketController
public abstract class WebsocketHandlers {
}
