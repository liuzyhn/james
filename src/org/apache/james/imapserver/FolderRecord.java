/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.util.Set;
import java.util.Map;

/**
 * Interface for objects representing the record of a folder on an IMAP host.
 * 
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */

public interface FolderRecord {
	
	
    /**
     * Returns the full name, including namespace, of this mailbox. 
     * Example 1: '#mail.projectBonzi'
     * Example 2: '#shared.projectBonzi'
     *
     * @returns String mailbox hierarchical name including namespace
     */
    String getFullName();

    /**
     * Returns the user in whose namespace the mailbox existed.
     * Example 1: 'fred.flintstone'
     * Example 2: ''
     *
     * @param user String a user.An empty string indicates that the 
     * mailbox name is absolute.
     */
    String getUser();

    /**
     * Returns the absolute name of this mailbox. The absolute name is
     * user-independent and unique for a given host.
     * Example 1: 'privatemail.fred.flintstone.projectBonzi'
     * Example 2: '#shared.projectBonzi'
     *
     * @returns String mailbox absolute name
     */
    String getAbsoluteName();

    /**
     * Records if this mailbox name is currently in use. The mailbox name is
     * in use when a mailbox with this name has been created. Implementations
     * that allow shared mailboxes may encounter a sate where the mailbox has
     * been deleted but there are clients who were already connected to the
     * mailbox. In this case the name remains in use until all clients have
     * either de-selected the mailbox or been disconnected from the server. 
     *
     * @param state boolean true when mailbox created, false when name no
     * longer in use.
     */
    void setNameInUse(boolean state);

    /**
     * Returns unavailability of name for a new mailbox.
     *
     * @returns true if this name is in use. Must return true if isDeleted
     * returns false.
     */
    boolean isNameInUse();

    /**
     *  Records if the corresponding mailbox has been deleted.
     *
     * @param state boolean true when mailbox deleted, false when created
     */
    void setDeleted(boolean state);

    /**
     * Returns whether mailbox has been deleted. A deleted mailbox is an
     * invalid argument to any IMAP command..
     *
     * @returns boolean true if mailbox does not exist
     */
    boolean isDeleted();

    /**
     * Records the Unique Identifier Validity Value for this mailbox.
     *
     * @param uidValidity int the uid validity value must be incremented if
     * the current uid values overlap uid values of this or a previous
     * incarnation of the mailbox.
     */
    void setUidValidity(int uidValidity);

    /**
     * Returns current uid validity value
     *
     * @returns int uid validity value
     */
    int getUidValidity();

   /**
     * Records the highest assigned Unique Identifier Value for this mailbox.
     *
     * @param uid int the highest uid assigned to a message in this mailbox.
     */
    void setHighestUid(int uid);

    /**
     * Returns current highest assigned uid value
     *
     * @returns int uid  value
     */
    int getHighestUid();

    /**
     * Record which users have LookupRights.
     *
     * @param users Set of Strings, one per user with Lookup rights
     */
    void setLookupRights(Set users);

    /**
     * Indicates if given user has lookup rights for this mailbox.  Need
     * lookup rights to be included in a List response.
     *
     * @returns boolean true if user has lookup rights
     */
    boolean hasLookupRights(String user);

    /**
     * Record which users have ReadRights.
     *
     * @param users Set of Strings, one per user with read rights
     */
    void setReadRights(Set users);

    /**
     * Indicates if given user has read rights for this mailbox. Need read
     * rights for user to select or examine mailbox.  
     *
     * @returns boolean true if user has read rights
     */
    boolean hasReadRights(String user);

   /**
     * Record if mailbox is marked.
     */
    void setMarked(boolean mark);

   /**
     * Indicates if the mailbox is marked. Usually means unseen mail.
     *
     * @returns boolean true if marked
     */
    boolean isMarked();

    /**
     * Mark this mailbox as not selectable by anyone. 
     * Example folders at the roots of hierarchies, e. #mail for each user.
     *
     * @param state true if folder is not selectable by anyone
     */
    void setNotSelectableByAnyone(boolean state);

    boolean isNotSelectableByAnyone();

    /**
     * A folder is selectable by a given user if both it is not
     * NotSelectableByAnyone and the named user has read rights.
     *
     * @parm user the user to be tested
     * @returns true if user can SELECT this mailbox.
     */
    boolean isSelectable(String user);

    /**
     * Set number of messages in this folder
     */
    void setExists(int num);

    /**
     * Indicates number of messages in folder
     *
     * @returns int number of messages
     */
    int getExists();

    /**
     * Set number of messages in this folder with Recent flag set
     */
    void setRecent(int num);

    /**
     * Indicates no of messages with \Recent flag set
     *
     * @returns int no of messages with \Recent flag set
     */
    int getRecent();

    /**
     * Set map of users versus number of messages in this folder without
     * \Seen flag set for them
     */
    void setUnseenbyUser(Map unseen);

    /** 
     * Indicates the number of  unseen messages for the specified user. 
     *
     * @returns int number of messages without \Seen flag set for this User.
     */
    int getUnseen(String user);
}
