package com.devicehive.domain;

/**
 * Created by tatyana on 2/10/15.
 */
public class ClusterConfig {

    private String metadataBrokerList;

    private String zookeeperConnect;

    private Integer threadsCount;

    private String cassandraContactpoints;

    public ClusterConfig(String metadataBrokerList, String zookeeperConnect, Integer threadsCount, String cassandraContactpoints) {
        this.metadataBrokerList = metadataBrokerList;
        this.zookeeperConnect = zookeeperConnect;
        this.threadsCount = threadsCount;
        this.cassandraContactpoints = cassandraContactpoints;
    }

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
