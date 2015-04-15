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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Collection;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public interface JElasticEngine {

    /** highlighter pre tags */
    public static final String HIGHLIGHTER_PRE_TAGS = "<font style='color:red'>";

    /** highlighter end tags */
    public static final String HIGHLIGHTER_END_TAGS = "</font>";


    /**
     * @Title: indexCreate
     * @deprecated:  crate single document to index , if {@code JElasticId} is not defined, created by es self,
     * but multi operate will make multi data stored in es ! if exists(_id equals), do update </br>
     * <pre>
     *     transportClient.prepareIndex(indexName, typeName).setSource(jsonObject.toString()).execute().actionGet();
     * </pre>
     * @author dennisit@163.com
     * @param indexName index name
     * @param typeName type name
     * @param source Object
     * @return success number
     */
    public int indexCreate(String indexName, String typeName, Object source);

    /**
     * @Title: indexCreate
     * @deprecated: bulk create collections object to index,here used prepareBulk() <br/>
     * <pre>
     *     method1: transportClient.prepareIndex(indexName, typeName).setSource(jsonObject.toString()).execute().actionGet();
     *              method1 the same as for each to execute createIndex(String indexName, String typeName,Object source);
     *     method2: transportClient.prepareBulk()..add(transportClient.prepareIndex(indexName, typeName).setSource(indexVal)).execute().actionGet();
     * </pre>
     * @author dennisit@163.com
     * @param collections collection data ready to index
     * @param indexName indexName
     * @param typeName indexType
     * @return total success number
     */
    public  int indexCreate(String indexName, String typeName, Collection<?> collections);


    /**
     * @Title: indexExists
     * @deprecated: check indexName is exists in index
     * @param indexNames one or more index name
     * @return existed return true, else retur false.
     */
    public boolean indexExists(String... indexNames);

    /**
     * @Title: select
     * @Description: select single document by document id
     * @author dennisit@163.com
     * @param indexName index name
     * @param typeName type name
     * @param id document id
     * @return String
     */
    public String select(String indexName, String typeName, String id);

    /**
     * @Title: select
     * @Description: select multi document by document id collection
     * @author dennisit@163.com
     * @param indexName index name
     * @param typeName type name
     * @param ids document id collections
     * @return String
     */
    public Collection<String> select(String indexName, String typeName, Collection<String> ids);

    /**
     * @Title: delete
     * @deprecated: delete single document by document id
     * @param indexName index name
     * @param typeName type name
     * @param id document id
     * @return total success number
     */
    public int delete(String indexName, String typeName, String id);

    /**
     * @Title: delete
     * @deprecated: delete multi document by document id collection
     * @param indexName index name
     * @param typeName type name
     * @param ids document id collection
     * @return total success number
     */
    public int delete(String indexName, String typeName, Collection<String> ids);

    /**
     * @Title: update
     * @deprecated: update object to index,not existed create, else delete and create <br/>
     * <pre>
     *     object ready to update, which should have document id identified with @JElasticId {@code JElasticId} </br>
     *     if id define with @JElasticId {@code JElasticId} ,direct call
     *     create(String indexName,String typeName,Object source)
     *     also can update ,which _id equals Object`s id identified with @JElasticId {@code JElasticId}
     * </pre>
     * @param indexName index name
     * @param typeName type name
     * @param source object
     * @return total success number
     */
    public int update(String indexName, String typeName, Object source);

    /**
     * @Title: update
     * @deprecated: multi update object to index,not existed create, else delete and create <br/>
     * @param indexName index name
     * @param typeName type name
     * @param collections multi object
     * @return total success number
     */
    public int update(String indexName, String typeName, Collection<?> collections);

    /**
     * @Title: merge
     * @deprecated: update object to index, not existed do create, else do update(merge means do not delete operate)
     * depend on _id identified with @JElasticId {@code JElasticId} <br/>
     * <pre>
     *     object ready to update, which should have document id identified with @JElasticId {@code JElasticId} </br>
     *     which _id should equals Object`s id identified with @JElasticId {@code JElasticId}
     * </pre>
     * @param indexName index name
     * @param typeName type name
     * @param source object
     * @return total success number
     */
    public int merge(String indexName, String typeName, Object source);

    /**
     * @Title: merge
     * @deprecated: multi update object to index, not existed do create, else do update(merge means do not delete operate)
     * depend on _id identified with @JElasticId {@code JElasticId} <br/>
     * @param indexName index name
     * @param typeName type name
     * @param collections multi object
     * @return total success number
     */
    public int merge(String indexName, String typeName, Collection<?> collections);

    /**
     * simple query support interface
     * @param indexName index name
     * @param typeName type name
     * @param keyword query key word
     * @param fields which field to query
     * @param size query size
     * @return result of data json Collection
     */
    public Collection<String> suggestSearch(String indexName, String typeName, String keyword, Collection<String> fields, Integer size);

    /**
     * Base search operate
     * @param indexName indexName
     * @param typeName typeName
     * @param searchType searchType, see {@Code SearchType}
     * @param queryBuilder search query, bind search keyword and search query field
     * @param filterBuilder search filter, just like range search
     * @param nowPage pager search page current
     * @param pageSize pager search page size
     * @param highFields which field used for highlighter
     * @return query result
     *
     */
    public SearchResponse search(String indexName, String typeName, SearchType searchType, QueryBuilder queryBuilder,
                                 FilterBuilder filterBuilder, Integer nowPage, Integer pageSize, Collection<String> highFields);



}
