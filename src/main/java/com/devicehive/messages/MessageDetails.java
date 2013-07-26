package com.devicehive.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.devicehive.model.User;

public class MessageDetails {

    private List<Long> ids;
    private Date timestamp;
    private String session;
    private Transport transport;
    private User user;

    public static MessageDetails create() {
        return new MessageDetails().transport(Transport.REST);
    }

    public MessageDetails ids(Long... ids) {
        this.ids = new ArrayList<>(Arrays.asList(ids));
        return this;
    }

    public MessageDetails ids(Collection<Long> ids) {
        this.ids = new ArrayList<>(ids);
        return this;
    }

    public MessageDetails timestamp(Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public MessageDetails session(String sessionId) {
        this.session = sessionId;
        return this;
    }

    public MessageDetails transport(Transport transport) {
        this.transport = transport;
        return this;
    }

    public MessageDetails user(User user) {
        this.user = user;
        return this;
    }

    public List<Long> ids() {
        return ids;
    }

    public Long id() {
        return ids != null && !ids.isEmpty() ? ids.get(0) : null;
    }

    public Date timestamp() {
        return timestamp;
    }

    public String session() {
        return session;
    }

    public Transport transport() {
        return transport;
    }

    public User user() {
        return user;
    }

}
