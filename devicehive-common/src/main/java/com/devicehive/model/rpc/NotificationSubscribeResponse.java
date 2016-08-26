package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Objects;

import static com.devicehive.configuration.Constants.*;

public class NotificationSubscribeResponse extends Body {
    private String subId;

    public NotificationSubscribeResponse(String subId) {
        super(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name());
        this.subId = subId;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationSubscribeResponse)) return false;
        if (!super.equals(o)) return false;

        NotificationSubscribeResponse that = (NotificationSubscribeResponse) o;

        return subId != null ? subId.equals(that.subId) : that.subId == null;

    }

    @Override
    public int hashCode() {
        return Objects.hash(action, subId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Body{");
        sb.append("action='").append(action).append("',");
        sb.append(SUBSCRIPTION_ID).append("='").append(subId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
