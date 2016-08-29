package com.devicehive.websockets.util;

import com.devicehive.websockets.events.WSMessageEvent;
import com.google.gson.JsonElement;
import com.lmax.disruptor.RingBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WSMessageSupplier {

    @Autowired
    private RingBuffer<WSMessageEvent> ringBuffer;

    public void deliver(JsonElement message, WebSocketSession session) {
        long sequence = ringBuffer.next();
        try
        {
            WSMessageEvent event = ringBuffer.get(sequence);
            event.setMessage(message);
            event.setSession(session);
        }
        finally
        {
            ringBuffer.publish(sequence);
        }
    }
}
