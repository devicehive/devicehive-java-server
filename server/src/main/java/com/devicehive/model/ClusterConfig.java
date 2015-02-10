package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_CLUSTER_CONFIG;

/**
 * Created by tatyana on 2/9/15.
 */
public class ClusterConfig implements HiveEntity {
    private static final long serialVersionUID = -4731922300811943546L;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("metadata.broker.list")
    private String metadataBrokerList;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("zookeeper.connect")
    private String zookeeperConnect;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("threads.count")
    private Integer threadsCount;

    @JsonPolicyDef(REST_CLUSTER_CONFIG)
    @SerializedName("cassandra.contactpoints")
    private String cassandraContactpoints;

    public String getMetadataBrokerList() {
        return metadataBrokerList;
    }

    public void setMetadataBrokerList(String metadataBrokerList) {
        this.metadataBrokerList = metadataBrokerList;
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

    public String getCassandraContactpoints() {
        return cassandraContactpoints;
    }

    public void setCassandraContactpoints(String cassandraContactpoints) {
        this.cassandraContactpoints = cassandraContactpoints;
    }
}
