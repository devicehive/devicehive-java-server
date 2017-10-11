package com.devicehive.proxy.api.payload;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import java.util.List;

public class TopicListPayload implements Payload {

    private List<TopicInfo> topicInfos;

    public TopicListPayload(List<TopicInfo> topicInfos) {
        this.topicInfos = topicInfos;
    }

    public List<TopicInfo> getTopicInfos() {
        return topicInfos;
    }

    public void setTopicInfos(List<TopicInfo> topicInfos) {
        this.topicInfos = topicInfos;
    }

    @Override
    public String toString() {
        return "TopicListPayload{" +
                "topicInfos=" + topicInfos +
                '}';
    }

    public class TopicInfo {

        private String topic;
        private Integer partition;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Integer getPartition() {
            return partition;
        }

        public void setPartition(Integer partition) {
            this.partition = partition;
        }

        @Override
        public String toString() {
            return "TopicInfo{" +
                    "topic='" + topic + '\'' +
                    ", partition=" + partition +
                    '}';
        }
    }
}
