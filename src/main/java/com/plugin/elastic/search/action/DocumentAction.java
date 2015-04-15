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
package com.plugin.elastic.search.action;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.plugin.elastic.search.annotation.JEAnalyzer;
import com.plugin.elastic.search.annotation.JElasticColumn;
import com.plugin.elastic.search.annotation.JElasticId;
import com.plugin.elastic.search.except.JElasticRunTimeException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class DocumentAction {

    public static final Logger LOG = LoggerFactory.getLogger(DocumentAction.class);

    /**
     * convertJsonString to JsonObject
     * @param jsonString
     * @return JsonObject
     */
    public JsonObject convertJsonStringToJsonObject(String jsonString) {
        if (StringUtils.isNotBlank(jsonString)) {
            try {
                return new JsonParser().parse(jsonString).getAsJsonObject();
            } catch (Exception e) {
                LOG.error("An exception occurred while converting json string to map object");
            }
        }
        return new JsonObject();
    }

    /**
     * convert object to json String
     * @param source object
     * @return json String
     */
    public String convertObjectToJsonString(Object source){
        return new Gson().toJson(source);
    }

    /**
     * scan annotation for object to elastic index
     * eg. {"id":"502","pin":"马云","desction":"电商、中国、创建","keyword":"阿里巴巴"}
     * @param source
     * @return
     */
    public String convertObjectToJelasticIndex(Object source){
        if (null == source){
            return null;
        }
        try {
            XContentBuilder json = XContentFactory.jsonBuilder();
            Class clazz = source.getClass();
            Field[] fields = clazz.getDeclaredFields();
            json.startObject();
            for (Field field : fields) {
                if(field.isAnnotationPresent(JElasticId.class) || field.isAnnotationPresent(JElasticColumn.class)){
                    field.setAccessible(true);
                    String ppName = field.getName();
                    Object ppVal =  field.get(source);
                    json.field(ppName,ppVal);
                }
            }
            return json.endObject().string();
        } catch (Exception e) {
            LOG.error("scan annotation for object to elastic index error." + e, e);
        }
        return null;
    }

    /**
     * get indexId from Object <br/>
     * <pre>
     *     object should have document id define with @JElasticId {@code JElasticId}
     * </pre>
     * @param source object with document id defined with @JElasticId {@code JElasticId}
     * @return document index id
     */
    public String getIndexIdFromSource(Object source) {
        if (source == null) return null;
        Field[] fields = source.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(JElasticId.class)) {
                try {
                    field.setAccessible(true);
                    Object name = field.get(source);
                    return name == null ? null : name.toString();
                } catch (IllegalAccessException e) {
                    LOG.error("Unhandled exception occurred while getting annotated id from source");
                }
            }
        }
        return null;
    }

    /**
     * get highlighter from result value
     * @param hit searchHit
     * @param field query field
     * @return
     */
    public String getHighlightFields(SearchHit hit, String field) {
        String content = "";
        if (hit != null) {
            Map<String, HighlightField> result = hit.highlightFields();
            HighlightField contentField = result.get(field);
            if (contentField != null) {
                Text[] contentTexts = contentField.fragments();
                for (Text text : contentTexts) {
                    content = text.toString();
                }
            } else {
                content = (String) hit.getSource().get(field);
            }
        }
        return content;
    }

    /**
     * elastic mapping schema
     * @param transportClient
     * @param indexName
     * @param typeName
     * @param source
     */
    protected void mappingSetting(TransportClient transportClient, String indexName, String typeName,Object source){
        XContentBuilder mapping = objectForMapping(typeName,source);
        try {
            LOG.debug("[mapping]" + mapping.string());
        } catch (IOException e) {
            LOG.error("[mapping]" + e.getMessage());
        }
        if(!isIndexExists(transportClient,indexName)){
            transportClient.admin().indices().prepareCreate(indexName).addMapping(typeName,mapping).execute().actionGet();
        }else{
            PutMappingRequest mappingRequest = Requests.putMappingRequest(indexName).type(typeName).source(mapping);
            transportClient.admin().indices().putMapping(mappingRequest).actionGet();
        }
    }

    /**
     * @param indexNames one or more index name
     * @return
     */
    protected boolean isIndexExists(TransportClient transportClient, String... indexNames){
        boolean isExists;
        ActionFuture<IndicesExistsResponse> response =  transportClient.admin().indices().exists(new IndicesExistsRequest(indexNames));
        try {
            StringBuffer buffer = new StringBuffer("[");
            if(indexNames.length>0){
                for(int i=0; i<indexNames.length; i++){
                    if(i<= indexNames.length-1){
                        buffer.append(indexNames[i]).append(",");
                    }else{
                        buffer.append(indexNames[i]);
                    }
                }
            }
            buffer.append("]");

            isExists = response.get().isExists();
            LOG.debug("[indexExists] " + buffer.toString() + " is Exists:" + isExists);

        } catch (Exception e) {
            throw new JElasticRunTimeException(e.getMessage());
        }
        return isExists;
    }

    /**
     * eg.
     * XContentBuilder mapping = XContentFactory.jsonBuilder()
     *    .startObject()
     *        .startObject("productIndex")
     *            .startObject("properties")
     *                    .startObject("title").field("type", "string").field("store", "yes").endObject()
     *                    .startObject("description").field("type", "string").field("index", "not_analyzed").endObject()
     *                    .startObject("price").field("type", "double").endObject()
     *                    .startObject("onSale").field("type", "boolean").endObject()
     *                    .startObject("type").field("type", "integer").endObject()
     *                    .startObject("createDate").field("type", "date").endObject()
     *            .endObject()
     *        .endObject()
     *    .endObject();
     * define elastic data mapping
     * @param source
     */
    public XContentBuilder objectForMapping(String typeName,Object source){
        if (null == source){
            return null;
        }
        XContentBuilder mapping = null;
        try {
            mapping = XContentFactory.jsonBuilder();
            mapping.startObject().startObject(typeName).startObject("properties");
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                String ppName = field.getName();
                String ppType = field.getType().toString();
                if(field.isAnnotationPresent(JElasticId.class)){
                    mapping.startObject(ppName).field("type","string").field("store", "yes").endObject();
                }
                if(field.isAnnotationPresent(JElasticColumn.class)){
                    JElasticColumn jElasticColumn =  field.getAnnotation(JElasticColumn.class);
                    mapping.startObject(ppName).field("type", javaTypeForElasticType(ppType));
                    if(jElasticColumn.instore()){
                        mapping.field("store", "yes");
                    }
                    if(jElasticColumn.analyzer() == JEAnalyzer.analyzed){
                        mapping.field("index", JEAnalyzer.analyzed);
                    }else if(jElasticColumn.analyzer() == JEAnalyzer.not_analyzed){
                        mapping.field("index", JEAnalyzer.not_analyzed);
                    }else if(jElasticColumn.analyzer() == JEAnalyzer.no){
                        mapping.field("index", JEAnalyzer.no);
                    }
                    mapping.endObject();
                }
            }
            mapping.endObject().endObject().endObject();
        } catch (IOException e) {
            LOG.error("scan annotation for construct elastic data mapping error");
        }
        return mapping;
    }


    protected String javaTypeForElasticType(String javaType){
        if( javaType.endsWith("Long") || javaType.endsWith("long") ){
            return "long";
        }
        if( javaType.endsWith("Integer") || javaType.endsWith("int")
                || javaType.endsWith("Short") || javaType.endsWith("short")
                || javaType.endsWith("Byte") || javaType.endsWith("byte") ){
            return "integer";
        }
        if( javaType.endsWith("Double") || javaType.endsWith("double") || javaType.endsWith("Float") || javaType.endsWith("float")
                || javaType.endsWith("BigDecimal") || javaType.endsWith("BigInteger") ) {
            return "double";
        }
        if( javaType.endsWith("Date") || javaType.endsWith("Timestamp") || javaType.endsWith("Time") ){
            return "date";
        }
        if( javaType.endsWith("Boolean") || javaType.endsWith("boolean") ){
            return "boolean";
        }
        return "string";
    }

}
