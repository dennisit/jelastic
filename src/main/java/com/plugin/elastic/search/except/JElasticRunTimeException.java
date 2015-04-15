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
package com.plugin.elastic.search.except;

/**
 * Description:
 * @author dennisit@163.com
 * @version 1.0
 */
public class JElasticRunTimeException extends RuntimeException {

    public JElasticRunTimeException() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JElasticRunTimeException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JElasticRunTimeException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JElasticRunTimeException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }


}
