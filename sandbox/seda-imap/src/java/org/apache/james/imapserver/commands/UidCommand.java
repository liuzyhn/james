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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;

/**
 * Handles processeing for the UID imap command.
 *
 * @version $Revision: 109034 $
 */
class UidCommand extends SelectedStateCommand
{
    public static final String NAME = "UID";
    public static final String ARGS = "<fetch-command>|<store-command>|<copy-command>|<search-command>";

    private ImapCommandFactory commandFactory;

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }

    public void setCommandFactory( ImapCommandFactory imapCommandFactory )
    {
        this.commandFactory = imapCommandFactory;
    }

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request, String tag) throws ProtocolException {
        String commandName = parser.atom( request );
        ImapCommand command = commandFactory.getCommand( commandName );
        if ( command == null ||
             ! (command instanceof UidEnabledCommand ) ) {
            throw new ProtocolException("Invalid UID command: '" + commandName + "'" );
        }
        final UidEnabledCommand uidEnabled = (UidEnabledCommand) command;
        final AbstractImapCommandMessage result = uidEnabled.decode( request, true, tag );
        return result;
    }
}
