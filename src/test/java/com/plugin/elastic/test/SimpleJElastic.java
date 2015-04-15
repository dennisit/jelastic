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
package com.plugin.elastic.test;

import com.google.gson.Gson;
import com.plugin.elastic.search.jelastic.JElasticRepository;
import com.plugin.elastic.test.bean.VModel;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class SimpleJElastic {


    ApplicationContext context = new ClassPathXmlApplicationContext("spring-bean-elastic.xml");

    TransportClient transportClient = null;

    JElasticRepository jElasticRepository = null;

    @Before
    public void init(){
        transportClient = (TransportClient) context.getBean("elasticClient");
        if(null != transportClient ){
            printf("init Elastic client finish!");
        }
        jElasticRepository = new JElasticRepository(transportClient);
        if(null != jElasticRepository ){
            printf("init Elastic Engine finish!");
        }
    }

    @Test
    public void createIndex(){
        VModel vmodel = new VModel("504","马云","电商、中国、创建","阿里巴巴222",new Date());
        printf("影响行数:" + jElasticRepository.indexCreate("test_index", "testName", vmodel));

    }

    @Test
    public void searchSuggest(){
        Set<String> fields = new HashSet<String>();
        fields.add("desction");
        fields.add("keyword");
        Collection<String> result = jElasticRepository.suggestSearch("test_index", "testName","中国",fields,5);
        printf("结果总数" + result.size());
        printf("推荐搜索" + result );
    }

    @Test
    public void search() throws IOException {
        Set<VModel> searhVm = new HashSet<VModel>();

        String indexName = "test_index";
        String typeName = "testName";
        String keyword = "中国";

        Set<String> fields = new HashSet<String>();
        fields.add("desction");
        fields.add("keyword");

        // 关键词转义
        QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(QueryParser.escape(keyword));
        for(String field : fields){
            queryBuilder.field(field);
        }
        SearchResponse searchResponse = jElasticRepository.search(indexName,typeName,null,queryBuilder, null,0,10,fields);

        for (SearchHit hit : searchResponse.getHits()) {
            //将文档中的每一个对象转换json串值
            String json = hit.getSourceAsString();
            //将json串值转换成对应的实体对象
            VModel vmodel = new ObjectMapper().readValue(json, VModel.class);
            vmodel.setDesction(jElasticRepository.getHighlightFields(hit, "desction"));
            vmodel.setKeyword(jElasticRepository.getHighlightFields(hit, "keyword"));
            searhVm.add(vmodel);
        }
        printf("[高亮]结果总数" + searhVm.size());
        printf("[高亮]推荐搜索" +jElasticRepository.convertObjectToJsonString(searhVm) );
    }


    @Test
    public void createBulkIndex(){
        Set<VModel> sets = new HashSet<VModel>();
        for(int i=0; i<3; i++){
            VModel plusPins = new VModel();
            plusPins.setId( (i+5) + "");
            plusPins.setPin("pin" + (i+5));
            plusPins.setKeyword("张三、李四"+i);
            plusPins.setDesction("测试数据");
            sets.add(plusPins);
        }
        printf("影响行数:" + jElasticRepository.indexCreate("test_index", "testName", sets));
    }

    @Test
    public void existIndex(){
        printf("" + jElasticRepository.indexExists("test_index"));
    }

    @Test
    public void getIndex(){
        printf(jElasticRepository.select("test_index","testName","502"));
    }

    @Test
    public void updateIndex(){
        VModel vmodel = new VModel();
        vmodel.setId("111");
        vmodel.setPin("dennisit2222");
        printf("影响行数:" + jElasticRepository.update("test_index", "testName", vmodel));
        printf(jElasticRepository.select("test_index","testName","111"));
    }

    @Test
    public void merge(){
        VModel vmodel = new VModel();
        vmodel.setId("11122");
        vmodel.setPin("dennisit666");
        printf("影响行数:" + jElasticRepository.merge("test_index", "testName", vmodel));
    }

    @Test
    public void multeGet(){
        Set<String> ids = new HashSet<String>();
        ids.add("5");
        ids.add("6");
        ids.add("7");
        printf("影响行数:" + jElasticRepository.select("test_index", "testName", ids));
    }

    @Test
    public void multeMerge(){
        Set<VModel> sets = new HashSet<VModel>();
        for(int i=0; i<3; i++){
            VModel plusPins = new VModel();
            plusPins.setId( (i+5) + "");
            plusPins.setPin("pin" + (2*i+5));
            sets.add(plusPins);
        }
        printf("影响行数:" + jElasticRepository.merge("test_index", "testName", sets));
    }

    /**
     {
         "test_index": {
             "properties": {
                 "id": {
                 "type": "string",
                 "store": "yes"
                 },
                 "pin": {
                     "type": "string",
                     "store": "yes",
                     "index": "analyzed"
                 },
                 "keyword": {
                     "type": "string",
                     "index": "analyzed"
                 },
                 "created": {
                     "type": "date",
                     "store": "yes",
                     "index": "analyzed"
                 }
            }
         }
     }
     */

    @Test
    public void testAnno(){
        XContentBuilder xContentBuilder = jElasticRepository.objectForMapping("test_index", new VModel());
        try {
            printf(xContentBuilder.string());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    @Test
    public void delete(){
        printf(new Gson().toJson(jElasticRepository.delete("test_index", "testName", "X7upWFSYSPencBiLMZRFfw")));
    }

    public void printf(String text){
        System.out.println("[TEST]" + text);
    }
}
