package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_CLUSTER_CONFIG;

/**
 * Created by tatyana on 2/9/15.
 */
public class ClusterConfigVO implements HiveEntity {
    private static final long serialVersionUID = -4731922300811943546L;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("bootstrap.servers")
    private String bootstrapServers;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("zookeeper.connect")
    private String zookeeperConnect;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("threads.count")
    private Integer threadsCount;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public Integer getThreadsCount() {
        return threadsCount;
    }

    public void setThreadsCount(Integer threadsCount) {
        this.threadsCount = threadsCount;
    }
}
