/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.userrepository;

import org.apache.james.security.DigestUtil;
import org.apache.james.services.User;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of User Interface. Instances of this class do not allow
 * the the user name to be reset.
 *
 *
 * @version CVS $Revision: 1.6.4.3 $
 */

public class DefaultUser implements User, Serializable {

    private String userName;
    private String hashedPassword;
    private String algorithm ;

    /**
     * Standard constructor.
     *
     * @param name the String name of this user
     * @param hashAlg the algorithm used to generate the hash of the password
     */
    public DefaultUser(String name, String hashAlg) {
        userName = name;
        algorithm = hashAlg;
    }

    /**
     * Constructor for repositories that are construcing user objects from
     * separate fields, e.g. databases.
     *
     * @param name the String name of this user
     * @param passwordHash the String hash of this users current password
     * @param hashAlg the String algorithm used to generate the hash of the
     * password
     */
    public DefaultUser(String name, String passwordHash, String hashAlg) {
        userName = name;
        hashedPassword = passwordHash;
        algorithm = hashAlg;
    }

    /**
     * Accessor for immutable name
     *
     * @return the String of this users name
     */
    public String getUserName() {
        return userName;
    }

    /**
     *  Method to verify passwords. 
     *
     * @param pass the String that is claimed to be the password for this user
     * @return true if the hash of pass with the current algorithm matches
     * the stored hash.
     */
    public boolean verifyPassword(String pass) {
        try {
            String hashGuess = DigestUtil.digestString(pass, algorithm);
            return hashedPassword.equals(hashGuess);
        } catch (NoSuchAlgorithmException nsae) {
        throw new RuntimeException("Security error: " + nsae);
    }
    }

    /**
     * Sets new password from String. No checks made on guessability of
     * password.
     *
     * @param newPass the String that is the new password.
     * @return true if newPass successfuly hashed
     */
    public boolean setPassword(String newPass) {
        try {
            hashedPassword = DigestUtil.digestString(newPass, algorithm);
            return true;
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Security error: " + nsae);
        }
    }

    /**
     * Method to access hash of password
     *
     * @return the String of the hashed Password
     */
    protected String getHashedPassword() {
        return hashedPassword;
    }

    /**
     * Method to access the hashing algorithm of the password.
     *
     * @return the name of the hashing algorithm used for this user's password
     */
    protected String getHashAlgorithm() {
        return algorithm;
    }


}
