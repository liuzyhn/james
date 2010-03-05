/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.transport.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.mailet.Mail;

/**
 * Check if the Mail state is NOT equal to the one given on the constructor
 * 
 *
 */
public class MailStateNotEquals implements Predicate{

    private String state;
    public MailStateNotEquals(String state) {
        this.state = state;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.camel.Predicate#matches(org.apache.camel.Exchange)
     */
    public boolean matches(Exchange ex) {
        Mail m = ex.getIn().getBody(Mail.class);
        if (state.equals(m.getState())) {
            return false;
        }
        return true;
    }

}
