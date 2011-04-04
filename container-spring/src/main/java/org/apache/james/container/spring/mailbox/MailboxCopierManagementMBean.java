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
package org.apache.james.container.spring.mailbox;

import java.util.Map;

import org.apache.james.mailbox.MailboxManager;

/**
 * Allow to copy {@link MailboxManager} contents from one to the other via JMX
 */
public interface MailboxCopierManagementMBean {

    /**
     * Return a {@link Map} which contains the bean name of the registered
     * {@link MailboxManager} instances as keys and the classname of them as
     * values
     * 
     * @return managers
     */
    Map<String, String> getMailboxManagerBeans();

    /**
     * Copy from srcBean to dstBean all messages
     * 
     * @param srcBean
     * @param dstBean
     * @return true if successful, false otherwise
     */
    void copy(String srcBean, String dstBean) throws Exception;

}
