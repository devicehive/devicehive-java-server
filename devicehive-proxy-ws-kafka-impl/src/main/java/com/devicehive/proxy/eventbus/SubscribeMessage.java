package com.devicehive.proxy.eventbus;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.google.gson.annotations.SerializedName;

class SubscribeMessage {

    @SerializedName("a")
    private int action;  // 0 - register, 1 - unregister todo: add enum for this field

    @SerializedName("f")
    private Filter filter;

    @SerializedName("s")
    private Subscriber subscriber;

    SubscribeMessage(SubscribeAction action, Filter filter, Subscriber subscriber) {
        this.action = action.getValue();
        this.filter = filter;
        this.subscriber = subscriber;
    }

    SubscribeMessage(SubscribeAction action, Subscriber subscriber) {
        this.action = action.getValue();
        this.subscriber = subscriber;
    }

    public SubscribeAction getAction() {
        return SubscribeAction.getValueForIndex(action);
    }

    public void setAction(SubscribeAction action) {
        this.action = action.getValue();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }
}
