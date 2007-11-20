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

package org.apache.james.mailboxmanager.wrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.AbstractGeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.EventQueueingSessionMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;

public abstract class NumberStableSessionWrapper extends AbstractGeneralMailbox implements EventQueueingSessionMailbox,
        MailboxListener {

    private static final class MsnIterator implements Iterator {
            private final Iterator it;
            private final UidToMsnBidiMap map;
            
            public MsnIterator(final Iterator it, final UidToMsnBidiMap map) {
                this.it = it;
                this.map = map;
            }
            
            public boolean hasNext() {
                return it.hasNext();
            }
    
            public Object next() {
                final MessageResult next = (MessageResult) it.next();
                final MessageResultImpl result = new MessageResultImpl(next);
                result.setMsn(map.getMsn(result.getUid()));
                return result;
            }
    
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        }

    protected GeneralMailbox mailbox;

    private UidToMsnBidiMap numberCache = null;
    
    protected Map flagEventMap = new TreeMap();

    protected SortedSet expungedEventList = new TreeSet();

    private MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();
    
    private boolean startingNumberCache = false;
    
    
    public NumberStableSessionWrapper() {
    }
    
    public NumberStableSessionWrapper(GeneralMailbox generalMailbox) throws MailboxManagerException {
        setMailbox(generalMailbox);
        init();
    }
    
    public void setMailbox(GeneralMailbox generalMailbox) {
        this.mailbox=generalMailbox;
    }

    public void init() throws MailboxManagerException {
        mailbox.addListener(eventDispatcher);
        eventDispatcher.addMailboxListener(this);        
    }

    protected UidToMsnBidiMap getNumberCache() throws MailboxManagerException {
        if (numberCache == null && mailbox != null) {
            startingNumberCache = true;
            try {
                final Iterator it = mailbox.getMessages(GeneralMessageSetImpl
                        .all(), MessageResult.UID);
                numberCache = new UidToMsnBidiMap();
                while (it.hasNext()) {
                    final MessageResult result = (MessageResult) it.next();
                    final long uid = result.getUid();
                    numberCache.add(uid);
                }
            } finally {
                startingNumberCache = false;
            }
        }
        return numberCache;
    }

    protected GeneralMessageSet toUidSet(GeneralMessageSet set)
            throws MailboxManagerException {

        if (set.getType() == GeneralMessageSet.TYPE_MSN) {
            set = GeneralMessageSetImpl.uidRange(getNumberCache().getUid(
                    set.getMsnFrom()), getNumberCache().getUid(set.getMsnTo()));
        }
        return set;
    }

    protected static int noMsnResult(int result) {
        if ((result & MessageResult.MSN) > 0) {
            result |= MessageResult.UID;
            result -= MessageResult.MSN;
        }
        return result;
    }

    protected MessageResult[] addMsnToResults(MessageResult[] mr, int result)
            throws MailboxManagerException {
        MessageResult[] translated = new MessageResult[mr.length];
        for (int i = 0; i < translated.length; i++) {
            translated[i] = addMsnResult(mr[i], result);
        }
        return translated;
    }

    protected MessageResult addMsnResult(MessageResult mr, int result)
            throws MailboxManagerException {
        // added events dispatched before the cache has been started
        // should be ignored
        if (mr != null) {
            if ((result & MessageResult.MSN) > 0) {
                // TODO copy the MessageResult because it could be another class
                int msn = getNumberCache().getMsn(mr.getUid());
                ((MessageResultImpl) mr).setMsn(msn);
            }
        }
        return mr;
    }

    public synchronized MessageResult[] getFlagEvents(boolean reset)
            throws MailboxManagerException {
        final MessageResult[] msnFlagEvents = buildMsnEvents(flagEventMap.values(),false);
        if (reset) {
            flagEventMap = new TreeMap();
        }
        return msnFlagEvents;
    }

    public synchronized Iterator getExpungedEvents(boolean reset)
            throws MailboxManagerException {
        final MessageResult[] msnExpungedEvents  = buildMsnEvents(expungedEventList,reset);
        if (reset) {
            expungedEventList = new TreeSet();
        } 
        return Arrays.asList(msnExpungedEvents).iterator();
    }

    protected MessageResult[]  buildMsnEvents(final Collection messageResults, 
            final boolean expunge)
            throws MailboxManagerException {
        final MessageResult[]  msnEvents = new MessageResult[messageResults.size()];
        int i=0;
        for (final Iterator iter = messageResults.iterator(); iter.hasNext();) {
            final MessageResult original = (MessageResult) iter.next();
            final MessageResultImpl newMr = new MessageResultImpl(original);
            final long uid = original.getUid();
            final UidToMsnBidiMap numberCache = getNumberCache();
            final int msn = numberCache.getMsn(uid);
            newMr.setMsn(msn);
            if (expunge) {
                numberCache.expunge(uid);
            }
            msnEvents[i++]=newMr;
        }
        return msnEvents;
    }

    public void added(MessageResult result) {
        try {
            // added events dispatched before the cache has been started
            // should be ignored
            if (!startingNumberCache && numberCache != null) 
            {
                getNumberCache().add(result.getUid());
            }
        } catch (MailboxManagerException e) {
            getLog().error("This should not happen",e);
        }
    }

    public void expunged(MessageResult mr) {
        getLog().debug("Expunged: "+mr);
        expungedEventList.add(mr);
    }

    public synchronized void flagsUpdated(MessageResult mr, long sessionId) {
        final long uid = mr.getUid();
        final Long uidObject = new Long(uid);
        if (sessionId != getSessionId() && !flagEventMap.containsKey(uidObject)) {
            // if there has been an external update in the past we should inform
            // about the newest value, even if in silent mode
            
            // only store flags
            final MessageResultImpl lightweightResult = new MessageResultImpl(uid);
            final Flags flags = mr.getFlags();
            lightweightResult.setFlags(flags);
            flagEventMap.put(uidObject, lightweightResult);
        }
    }

    
    
    /**
     * @see org.apache.james.mailboxmanager.MailboxListener#event(org.apache.james.mailboxmanager.MailboxListener.Event)
     */
    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final long sessionId = event.getSessionId();
            final MessageEvent messageEvent = (MessageEvent) event;
            final MessageResult result = messageEvent.getSubject();
            if (event instanceof Added) {
                added(result);
            } else if (event instanceof Expunged) {
                expunged(result);
            } else if (event instanceof FlagsUpdated) {
                flagsUpdated(result, sessionId);
            }
        }
    }
    
    public void mailboxDeleted() {
        // TODO Auto-generated method stub

    }

    public void mailboxRenamed(String origName, String newName) {
        // TODO Auto-generated method stub

    }
    
    public void addListener(MailboxListener listener) {
        eventDispatcher.addMailboxListener(listener);
    }

    public void removeListener(MailboxListener listener) {
        eventDispatcher.removeMailboxListener(listener);
    }
    
    public void close() {
        mailbox.removeListener(eventDispatcher);
        mailbox=null;
    }
    
    protected final Iterator addMsn(Iterator iterator) throws MailboxManagerException {
        return new MsnIterator(iterator, getNumberCache());
    }

    /**
     * for testing
     * @return the listener this class uses to subscribe to Mailbox events
     */
    
    MailboxListener getListenerObject() {
        return eventDispatcher;
    }

}
