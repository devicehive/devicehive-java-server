package com.devicehive.websockets;

import com.devicehive.websockets.events.WSMessageEvent;
import com.google.gson.JsonObject;
import com.lmax.disruptor.RingBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WSMessageProducer {

    @Autowired
    private RingBuffer<WSMessageEvent> ringBuffer;

    public void onData(JsonObject request, WebSocketSession session)
    {
        long sequence = ringBuffer.next();
        try
        {
            WSMessageEvent event = ringBuffer.get(sequence);
            event.setRequest(request);
            event.setSession(session);
        }
        finally
        {
            ringBuffer.publish(sequence);
        }
    }

}
