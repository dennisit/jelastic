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
package com.plugin.elastic.test.bean;

import com.plugin.elastic.search.annotation.JEAnalyzer;
import com.plugin.elastic.search.annotation.JElasticColumn;
import com.plugin.elastic.search.annotation.JElasticId;

import java.io.Serializable;
import java.util.Date;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class VModel implements Serializable {

    @JElasticId
    private String id;

    @JElasticColumn
    private String pin;

    private String desction;

    @JElasticColumn(instore = false,analyzer = JEAnalyzer.not_analyzed)
    private String keyword;

    @JElasticColumn(instore = true)
    private Date created;


    public VModel() {
    }

    public VModel(String id, String pin, String desction, String keyword, Date date) {
        this.id = id;
        this.pin = pin;
        this.desction = desction;
        this.keyword = keyword;
        this.created = date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDesction() {
        return desction;
    }

    public void setDesction(String desction) {
        this.desction = desction;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}

