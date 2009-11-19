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
package org.apache.james.management.impl;

import org.apache.james.services.SpoolManager;
import org.apache.james.management.ProcessorManagementMBean;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.management.mbean.MatcherManagement;
import org.apache.james.management.mbean.ProcessorDetail;
import org.apache.james.management.mbean.MailetManagement;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MatcherConfig;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * manage processors, mailets and matchers
 */
public class ProcessorManagement implements ProcessorManagementService, ProcessorManagementMBean {

    private SpoolManager processorManager;

    @PostConstruct
    public void init() {
        registerMBeans();
    }
    
    @SuppressWarnings("unchecked")
    private void registerMBeans() {
        ArrayList<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (mBeanServers == null || mBeanServers.size() == 0) return; // no server to publish MBeans

        // take the first one
        MBeanServer mBeanServer = mBeanServers.get(0);

        String baseObjectName = "Phoenix:application=james,topic=ProcessorAdministration,block=ProcessorManagement,";

        String[] processorNames = getProcessorNames();
        for (int i = 0; i < processorNames.length; i++) {
            String processorName = processorNames[i];
            createProcessorMBean(baseObjectName, processorName, mBeanServer);
            continue;
        }
    }

    private void createProcessorMBean(String baseObjectName, String processorName, MBeanServer mBeanServer) {
        String processorMBeanName = baseObjectName + "processor=" + processorName;
        ProcessorDetail processorMBean = new ProcessorDetail(processorManager, processorName);
        registerMBean(mBeanServer, processorMBeanName, processorMBean);

        // add all mailets but the last, because that is a terminator (see LinearProcessor.closeProcessorLists())
        List<MailetConfig> mailetConfigs = processorManager.getMailetConfigs(processorName);
        for (int i = 0; i < mailetConfigs.size()-1; i++) {
            MailetConfig mailetConfig = mailetConfigs.get(i);

            String mailetMBeanName = processorMBeanName + ",subtype=mailet,index=" + (i+1) + ",mailetname=" + mailetConfig.getMailetName();
            MailetManagement mailetMBean = new MailetManagement(mailetConfig);
            registerMBean(mBeanServer, mailetMBeanName, mailetMBean);
        }

        // add all matchers but the last, because that is a terminator (see LinearProcessor.closeProcessorLists())
        List<MatcherConfig> matcherConfigs = processorManager.getMatcherConfigs(processorName);
        for (int i = 0; i < matcherConfigs.size()-1; i++) {
            MatcherConfig matcherConfig = matcherConfigs.get(i);

            String matcherMBeanName = processorMBeanName + ",subtype=matcher,index=" + (i+1) + ",matchername=" + matcherConfig.getMatcherName();
            MatcherManagement matcherMBean = new MatcherManagement(matcherConfig);
            registerMBean(mBeanServer, matcherMBeanName, matcherMBean);
        }

    }

    private void registerMBean(MBeanServer mBeanServer, String mBeanName, Object object) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(mBeanName);
        } catch (MalformedObjectNameException e) {
            return; // TODO handle error, log something
        }
        try {
            mBeanServer.registerMBean(object, objectName);
        } catch (javax.management.JMException e) {
            e.printStackTrace(); // TODO change to logger
        }
    }

    @Resource(name="org.apache.james.services.SpoolManager")
    public void setProcessorManager(SpoolManager processorManager) {
        this.processorManager = processorManager;
    }

    /**
     * @see org.apache.james.management.ProcessorManagementMBean#getProcessorNames()
     */
    public String[] getProcessorNames() {
        return processorManager.getProcessorNames();
    }

    /**
     * @see org.apache.james.management.ProcessorManagementService#getMailetNames(java.lang.String)
     */
    public String[] getMailetNames(String processorName) {
        List<MailetConfig> mailetConfigs = processorManager.getMailetConfigs(processorName);
        // always ommit the terminating mailet
        String[] mailetNames = new String[mailetConfigs.size()-1];
        int i = 0;
        Iterator<MailetConfig> iterator = mailetConfigs.iterator();
        while (iterator.hasNext()) {
            MailetConfig mailetConfig = iterator.next();
            if (!iterator.hasNext()) continue; // ommit the terminating mailet
            String mailetName = mailetConfig.getMailetName();
            mailetNames[i] = mailetName;
            i++;
        }
        return mailetNames;
    }

    /**
     * @see org.apache.james.management.ProcessorManagementService#getMatcherNames(java.lang.String)
     */
    public String[] getMatcherNames(String processorName) {
        List<MatcherConfig> matcherConfigs = processorManager.getMatcherConfigs(processorName);
        // always ommit the terminating mailet
        String[] matcherNames = new String[matcherConfigs.size()-1];
        int i = 0;
        Iterator<MatcherConfig> iterator = matcherConfigs.iterator();
        while (iterator.hasNext()) {
            MatcherConfig matcherConfig = iterator.next();
            if (!iterator.hasNext()) continue; // ommit the terminating mailet
            String matcherName = matcherConfig.getMatcherName();
            matcherNames[i] = matcherName;
            i++;
        }
        return matcherNames;
    }
    
    /**
     * @see org.apache.james.management.ProcessorManagementService#getMatcherParameters(java.lang.String, int)
     */
    public String[] getMatcherParameters(String processorName, int matcherIndex) {
        List<MatcherConfig> matcherConfigs = processorManager.getMatcherConfigs(processorName);
        if (matcherConfigs == null || matcherConfigs.size() < matcherIndex) return null;
        MatcherConfig matcherConfig = matcherConfigs.get(matcherIndex);
        return new String[] {matcherConfig.getCondition()};
    }

    /**
     * @see org.apache.james.management.ProcessorManagementService#getMailetParameters(java.lang.String, int)
     */
    public String[] getMailetParameters(String processorName, int mailetIndex) {
        List<MailetConfig> mailetConfigs = processorManager.getMailetConfigs(processorName);
        if (mailetConfigs == null || mailetConfigs.size() < mailetIndex) return null;
        MailetConfig mailetConfig = mailetConfigs.get(mailetIndex);
        return MailetManagement.getMailetParameters(mailetConfig);
    }

}
