//--------------------------------------------------------------------------
//	Copyright (c) 2010-2020, En.dennisit or Cn.苏若年
//  All rights reserved.
//
//	Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are
//  met:
//
//	Redistributions of source code must retain the above copyright notice,
//  this list of conditions and the following disclaimer.
//	Redistributions in binary form must reproduce the above copyright
//  notice, this list of conditions and the following disclaimer in the
//  documentation and/or other materials provided with the distribution.
//	Neither the name of the dennisit nor the names of its contributors
//  may be used to endorse or promote products derived from this software
//  without specific prior written permission.
//  Author: dennisit@163.com | dobby | 苏若年
//--------------------------------------------------------------------------
package com.plugin.elastic.search;

import com.plugin.elastic.search.except.JElasticRunTimeException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.FactoryBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class ElasticFactoryBean implements FactoryBean, Serializable {

    private String clusterName = "elasticsearch" ;

    private String discoveryType = "zen" ;

    private Integer discoveryZenMinMasterNodes = 2 ;

    private Integer discoveryZenPingTimeout = 200 ;

    private Integer discoveryInitialStateTimeout = 500 ;

    /**local | fs |  none | local*/
    private String gatewayType =  "local";

    private Integer indexNumberOfShards = 1;

    private Boolean autoCrateIndex = false;

    private Integer clusterRoutingSchedule = 50;

    /** 服务器相对域地址 */
    private String serverAddress = "localhost:9300";

    private TransportClient client = null;

    public TransportClient initClient(){
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("cluster.name", this.clusterName)
                .put("discovery.type", this.discoveryType)
                .put("discovery.zen.minimum_master_nodes", this.discoveryZenMinMasterNodes)
                .put("discovery.zen.ping_timeout", this.discoveryZenPingTimeout + "ms")
                .put("discovery.initial_state_timeout", this.discoveryInitialStateTimeout + "ms")
                .put("gateway.type", this.gatewayType)//(fs, none, local)
                .put("index.number_of_shards", this.indexNumberOfShards)
                .put("action.auto_create_index", this.autoCrateIndex)
                .put("cluster.routing.schedule", this.clusterRoutingSchedule + "ms")//发现新节点时间
                .build();

        // 集群地址配置
        List<InetSocketTransportAddress> list = new ArrayList<InetSocketTransportAddress>();
        if (StringUtils.isNotEmpty(serverAddress)) {
            String[] strArr = this.serverAddress.split(",");
            for (String str : strArr) {
                String[] addressAndPort = str.split(":");
                String address = addressAndPort[0];
                int port = Integer.valueOf(addressAndPort[1]);
                InetSocketTransportAddress inetSocketTransportAddress = new InetSocketTransportAddress(address, port);
                list.add(inetSocketTransportAddress);
            }
        }
        // 这里可以同时连接集群的服务器,可以多个,并且连接服务是可访问的
        InetSocketTransportAddress addressList[] = list.toArray(new InetSocketTransportAddress[list.size()]);
        return new TransportClient(settings).addTransportAddresses(addressList);

    }

    @Override
    public Object getObject() throws Exception {
        if(StringUtils.isBlank(this.serverAddress)){
            throw new JElasticRunTimeException("init TransportAddress is null!");
        }
        if(this.serverAddress.indexOf(":") == -1){
            throw new JElasticRunTimeException("illegal server address, should be follow : 192.168.154.73:9300,192.168.154.74:9300");
        }
        if(null == this.client){
            this.client = initClient();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return TransportClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void close(){
        this.client.close();
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setDiscoveryType(String discoveryType) {
        this.discoveryType = discoveryType;
    }

    public void setDiscoveryZenMinMasterNodes(Integer discoveryZenMinMasterNodes) {
        this.discoveryZenMinMasterNodes = discoveryZenMinMasterNodes;
    }

    public void setDiscoveryZenPingTimeout(Integer discoveryZenPingTimeout) {
        this.discoveryZenPingTimeout = discoveryZenPingTimeout;
    }

    public void setDiscoveryInitialStateTimeout(Integer discoveryInitialStateTimeout) {
        this.discoveryInitialStateTimeout = discoveryInitialStateTimeout;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public void setIndexNumberOfShards(Integer indexNumberOfShards) {
        this.indexNumberOfShards = indexNumberOfShards;
    }

    public void setAutoCrateIndex(Boolean autoCrateIndex) {
        this.autoCrateIndex = autoCrateIndex;
    }

    public void setClusterRoutingSchedule(Integer clusterRoutingSchedule) {
        this.clusterRoutingSchedule = clusterRoutingSchedule;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
