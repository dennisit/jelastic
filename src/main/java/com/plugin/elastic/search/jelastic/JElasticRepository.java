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
package com.plugin.elastic.search.jelastic;

import com.plugin.elastic.search.action.DocumentAction;
import com.plugin.elastic.search.except.JElasticRunTimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class JElasticRepository extends DocumentAction implements JElasticEngine {

    public static final Logger LOG = LoggerFactory.getLogger(JElasticRepository.class);

    private TransportClient transportClient;

    /**
     * construct rely on
     * @param transportClient
     */
    public JElasticRepository(TransportClient transportClient){
        this.transportClient = transportClient;
    }

    /**
     * @param indexName index name
     * @param typeName type name
     * @param source Object
     * @return
     */
    @Override
    public int indexCreate(String indexName, String typeName,Object source){
        mappingSetting(transportClient,indexName,typeName,source);
        int successResult = 0;
        String indexVal = convertObjectToJelasticIndex(source);
        String indexId = getIndexIdFromSource(source);
        try{
            LOG.debug("[createIndex] [prepareIndex] indexName:{}, typeName:{}, indexId:{}, indexValue:{}", new Object[]{indexName,typeName,indexId,indexVal});
            if(StringUtils.isNotBlank(indexId)){
                transportClient.prepareIndex(indexName,typeName,indexId).setSource(indexVal).execute().actionGet();
            }else{
                transportClient.prepareIndex(indexName,typeName).setSource(indexVal).execute().actionGet();
            }
            successResult = successResult + 1;
        }catch (Exception e){
            LOG.error("[createIndex] [prepareIndex] indexName:{}, typeName:{}, object：{} error, error message:{}", new Object[]{
                    indexName,typeName,indexVal,e.getMessage()} ,e);
        }
        return successResult;
    }

    /**
     * @param indexName indexName
     * @param typeName indexType
     * @param collections collection data ready to index
     * @return
     */
    @Override
    public int indexCreate(String indexName, String typeName,Collection<?> collections) {
        int successResult = 0;
        BulkRequestBuilder bulkRequest = transportClient.prepareBulk();
        for (Object source: collections) {
            successResult++;
            String indexVal = convertObjectToJelasticIndex(source);
            String indexId = getIndexIdFromSource(source);
            LOG.debug("[createIndex] [bulkRequest] indexName:{}, typeName:{}, indexId:{}, indexValue:{}", new Object[]{indexName,typeName,indexId,indexVal});
            mappingSetting(transportClient,indexName,typeName,source);
            if(StringUtils.isNotBlank(indexId)){
                bulkRequest.add(transportClient.prepareIndex(indexName, typeName, indexId).setSource(indexVal));
            }else{
                bulkRequest.add(transportClient.prepareIndex(indexName, typeName).setSource(indexVal));
            }
        }
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (!bulkResponse.hasFailures()) {
            return successResult;
        } else {
            LOG.error("[createIndex] [bulkRequest] indexName:{}, typeName:{}, error:{}", new Object[]{
                    indexName,typeName,bulkResponse.buildFailureMessage()});
        }
        return successResult;
    }

    /**
     * @param indexNames one or more index name
     * @return
     */
    @Override
    public boolean indexExists(String... indexNames){
        return super.isIndexExists(transportClient,indexNames);
    }

    /**
     *
     * @param indexName index name
     * @param typeName type name
     * @param id document id
     * @return
     */
    @Override
    public String select(String indexName, String typeName, String id) {
        LOG.debug("[select] indexName:{}, typeName:{}, indexId:{}", new Object[]{indexName,typeName,id});
        GetResponse response = transportClient.prepareGet(indexName, typeName, id).execute().actionGet();
        LOG.debug("response.getSource()：" + response.getSource());
        LOG.debug("response.getId():" + response.getId());
        LOG.debug("response.getSourceAsString():" + response.getSourceAsString());
        return response.getSourceAsString();
    }

    /**
     *
     * @param indexName index name
     * @param typeName type name
     * @param ids document id collections
     * @return
     */
    @Override
    public Collection<String> select(String indexName, String typeName, Collection<String> ids) {
        LOG.debug("[select] indexName:{}, typeName:{}, indexIds:{}", new Object[]{indexName,typeName, ids});
        Collection<String> sets = new CopyOnWriteArraySet<String>();
        if( ids == null || ids.isEmpty() ){
            return null;
        }
        for(String id: ids){
            sets.add(select(indexName,typeName,id));
        }
        return sets;
    }


    /**
     * @param indexName index name
     * @param typeName type name
     * @param id document id
     * @return
     */
    @Override
    public int delete(String indexName, String typeName, String id){
        LOG.debug("[delete] indexName:{}, typeName:{}, indexId:{}", new Object[]{indexName,typeName,id});
        int successResult = 0;
        try {
            DeleteResponse response = transportClient.prepareDelete(indexName,typeName,id).execute().actionGet();
            successResult = successResult + 1;
            LOG.debug("response.getId():" + response.getId());
            LOG.debug("response.isFound():" + response.isFound()); // 返回索引是否存在,存在删除
        } catch (Exception e) {
            throw new JElasticRunTimeException(e.getMessage());
        }
        return successResult;
    }

    @Override
    public int delete(String indexName, String typeName, Collection<String> ids) {
        LOG.debug("[delete] indexName:{}, typeName:{}, indexIds:{}", new Object[]{indexName,typeName, ids});
        int successResult = 0;
        if( ids == null || ids.isEmpty() ){
            return 0;
        }
        for(String id : ids){
            delete(indexName,typeName,id);
            successResult  = successResult + 1;
        }
        return successResult;
    }

    /**
     * @param indexName index name
     * @param typeName type name
     * @param source object
     * @return
     */
    @Override
    public int update(String indexName,String typeName,Object source) {
        String indexId = getIndexIdFromSource(source);
        LOG.debug("[update]indexName:{}, typeName:{}, indexId:{}, indexValue:{}", new Object[]{indexName,typeName,indexId,convertObjectToJelasticIndex(source)});
        if(StringUtils.isBlank(indexId)){
            LOG.error("[update]get indexId from object source error, null indexId is found!");
            return 0;
        }
        // this step can skip if "_id" identified with @JElasticId {@code JElasticId}
        delete(indexName,typeName,indexId);
        return indexCreate(indexName, typeName, source);
    }

    /**
     *
     * @param indexName index name
     * @param typeName type name
     * @param collections multi object
     * @return
     */
    @Override
    public int update(String indexName, String typeName, Collection<?> collections) {
        LOG.debug("[update] indexName:{}, typeName:{}, objects:{}", new Object[]{indexName,typeName, convertObjectToJelasticIndex(collections)});
        int successResult = 0;
        if( collections == null || collections.isEmpty() ){
            return 0;
        }
        for(Object source : collections){
            update(indexName,typeName,source);
            successResult  = successResult + 1;
        }
        return successResult;
    }

    /**
     * @param indexName index name
     * @param typeName type name
     * @param source object
     * @return
     */
    @Override
    public int merge(String indexName,String typeName,Object source) {
        mappingSetting(transportClient,indexName,typeName,source);
        String indexId = getIndexIdFromSource(source);
        LOG.debug("[merge]indexName:{}, typeName:{}, indexId:{}, indexValue:{}", new Object[]{indexName,typeName,indexId,convertObjectToJelasticIndex(source)});
        if(StringUtils.isBlank(indexId)){
            LOG.error("[merge]get indexId from object source error, null indexId is found!");
            return 0;
        }
        return indexCreate(indexName,typeName,source);
    }

    /**
     *
     * @param indexName
     * @param typeName
     * @param collections
     * @return
     */
    @Override
    public int merge(String indexName, String typeName, Collection<?> collections) {
        LOG.debug("[merge] indexName:{}, typeName:{}, objects:{}", new Object[]{indexName,typeName, convertObjectToJelasticIndex(collections)});
        int successResult = 0;
        if( collections == null || collections.isEmpty() ){
            return 0;
        }
        for(Object source : collections){
            merge(indexName,typeName,source);
            successResult  = successResult + 1;
        }
        return successResult;
    }

    @Override
    public Collection<String> suggestSearch(String indexName,String typeName,String keyword, Collection<String> fields, Integer size) {
        if(null== fields || fields.isEmpty()){
            return null;
        }
        Set<String> sets = new CopyOnWriteArraySet<String>();
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(indexName);
        searchRequestBuilder.setTypes(typeName);
        // keyword escape
        QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(QueryParser.escape(keyword));
        for(String field : fields){
            queryBuilder.field(field);
        }
        /*
        TermsBuilder termsBuilder = AggregationBuilders.terms(keyword);
        for(String field : fields){
            termsBuilder.field(field);
        }

        SearchResponse searchResponse = transportClient.prepareSearch(indexName).setTypes(indexType)
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(termsBuilder).setSize(size).setExplain(true).execute().actionGet();
        */
        SearchResponse searchResponse = search(indexName,typeName, null,queryBuilder,null,0,size,null);
        long matchTotal = searchResponse.getHits().totalHits();
        LOG.debug("match total rows:" + matchTotal);
        for (SearchHit hit : searchResponse.getHits()) {
            sets.add(hit.getSourceAsString());
        }
        return sets;
    }

    /**
     *
     * @param indexName indexName
     * @param typeName typeName
     * @param searchType searchType, see {@Code SearchType}
     * @param queryBuilder search query, bind search keyword and search query field
     * @param filterBuilder search filter, just like range search
     * @param nowPage pager search page current
     * @param pageSize pager search page size
     * @param highFields which field used for highlighter
     * @return
     */
    @Override
    public SearchResponse search(String indexName, String typeName, SearchType searchType, QueryBuilder queryBuilder, FilterBuilder filterBuilder, Integer nowPage, Integer pageSize,Collection<String> highFields) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(indexName).setTypes(typeName);
        if( null != searchType ){
            searchRequestBuilder.setSearchType(searchType);
        }
        if( null != queryBuilder ){
            searchRequestBuilder.setQuery(queryBuilder);
        }else{
            searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        }
        if( null != filterBuilder ){
            searchRequestBuilder.setPostFilter(filterBuilder);
        }
        if( null != nowPage ){
            searchRequestBuilder.setFrom(0);
        }
        if( null != pageSize){
            searchRequestBuilder.setSize(pageSize);
        }
        if( !(null == highFields || highFields.isEmpty()) ){
            for (String field : highFields) {
                searchRequestBuilder.addHighlightedField(field);
            }
            searchRequestBuilder.setHighlighterPreTags(HIGHLIGHTER_PRE_TAGS);
            searchRequestBuilder.setHighlighterPostTags(HIGHLIGHTER_END_TAGS);
        }
        SearchResponse searchResponse = searchRequestBuilder.setExplain(true).execute().actionGet();
        return searchResponse;
    }



    public void setTransportClient(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

}
