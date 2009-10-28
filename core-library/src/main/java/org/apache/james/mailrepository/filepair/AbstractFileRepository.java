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



package org.apache.james.mailrepository.filepair;

import org.apache.avalon.cornerstone.services.store.Repository;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.services.FileSystem;
import org.apache.james.util.io.ExtensionFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * This an abstract class implementing functionality for creating a file-store.
 *
 */
public abstract class AbstractFileRepository
    implements Repository {
    protected static final boolean DEBUG = false;

    protected static final String HANDLED_URL = "file://";
    protected static final int BYTE_MASK = 0x0f;
    protected static final char[] HEX_DIGITS = new char[]
    {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    protected String m_path;
    protected String m_destination;
    protected String m_extension;
    protected String m_name;
    protected FilenameFilter m_filter;
    protected File m_baseDirectory;

    private FileSystem fileSystem;

    private Log logger;

    private HierarchicalConfiguration configuration;

    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLogger(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }
      
    
    protected abstract String getExtensionDecorator();



    protected void doConfigure( final HierarchicalConfiguration configuration )
        throws ConfigurationException
    {
        if( null == m_destination )
        {
            final String destination = configuration.getString( "[@destinationURL]" );
            setDestination( destination );
        }
    }

    @PostConstruct
    public void init()
        throws Exception
    {
        getLogger().info( "Init " + getClass().getName() + " Store" );

        doConfigure(configuration);
        try {
            m_baseDirectory = fileSystem.getBasedir();
        } catch (FileNotFoundException e) {
            getLogger().error("Cannot find the base directory of the application",e);
            throw new FileNotFoundException("Cannot find the base directory of the application");
        }
        

        m_name = "Repository";
        String m_postfix = getExtensionDecorator();
        m_extension = "." + m_name + m_postfix;
        m_filter = new ExtensionFileFilter(m_extension);
        //m_filter = new NumberedRepositoryFileFilter(getExtensionDecorator());

        final File directory = new File( m_path );
        directory.mkdirs();

        getLogger().info( getClass().getName() + " opened in " + m_path );

        //We will look for all numbered repository files in this
        //  directory and rename them to non-numbered repositories,
        //  logging all the way.

        FilenameFilter num_filter = new NumberedRepositoryFileFilter(getExtensionDecorator());
        final String[] names = directory.list( num_filter );

        try {
            for( int i = 0; i < names.length; i++ ) {
                String origFilename = names[i];

                //This needs to handle (skip over) the possible repository numbers
                int pos = origFilename.length() - m_postfix.length();
                while (pos >= 1 && Character.isDigit(origFilename.charAt(pos - 1))) {
                    pos--;
                }
                pos -= ".".length() + m_name.length();
                String newFilename = origFilename.substring(0, pos) + m_extension;

                File origFile = new File(directory, origFilename);
                File newFile = new File(directory, newFilename);

                if (origFile.renameTo(newFile)) {
                    getLogger().info("Renamed " + origFile + " to " + newFile);
                } else {
                    getLogger().info("Unable to rename " + origFile + " to " + newFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Set the destination for the repository
     * 
     * @param destination the destination under which the repository get stored
     * @throws ConfigurationException get thrown on invalid destintion syntax
     */
    protected void setDestination( final String destination )
        throws ConfigurationException
    {
        if( !destination.startsWith( HANDLED_URL ) )
        {
            throw new ConfigurationException( "cannot handle destination " + destination );
        }

        m_path = destination.substring( HANDLED_URL.length() );

        File directory;

        // Check for absolute path
        if( m_path.startsWith( "/" ) )
        {
            directory = new File( m_path );
        }
        else
        {
            directory = new File( m_baseDirectory, m_path );
        }

        try
        {
            directory = directory.getCanonicalFile();
        }
        catch( final IOException ioe )
        {
            throw new ConfigurationException( "Unable to form canonical representation of " +
                                              directory );
        }

        m_path = directory.toString();

        m_destination = destination;
    }

    /**
     * Return a new instance of this class
     * 
     * @return class a new instance of AbstractFileRepository
     * @throws Exception get thrown if an error is detected while create the new instance
     */
    protected AbstractFileRepository createChildRepository()
        throws Exception
    {
        return (AbstractFileRepository) getClass().newInstance();
    }

    /**
     * @see org.apache.avalon.cornerstone.services.store.Repository#getChildRepository(String)
     */
    public Repository getChildRepository( final String childName )
    {
        AbstractFileRepository child = null;

        try
        {
            child = createChildRepository();
        }
        catch( final Exception e )
        {
            throw new RuntimeException( "Cannot create child repository " +
                                        childName + " : " + e );
        }

        child.setFileSystem(fileSystem);
        child.setLogger(logger);

        try
        {
            child.setDestination( m_destination + File.pathSeparatorChar +
                                  childName + File.pathSeparator );
        }
        catch( final ConfigurationException ce )
        {
            throw new RuntimeException( "Cannot set destination for child child " +
                                        "repository " + childName +
                                        " : " + ce );
        }

        try
        {
            child.init();
        }
        catch( final Exception e )
        {
            throw new RuntimeException( "Cannot initialize child " +
                                        "repository " + childName +
                                        " : " + e );
        }

        if( DEBUG )
        {
            getLogger().debug( "Child repository of " + m_name + " created in " +
                               m_destination + File.pathSeparatorChar +
                               childName + File.pathSeparator );
        }

        return child;
    }

    /**
     * Return the File Object which belongs to the given key
     * 
     * @param key  the key for which the File get returned
     * @return file the File associted with the given Key
     * @throws IOException get thrown on IO error
     */
    protected File getFile( final String key )
        throws IOException
    {
        return new File( encode( key ) );
    }

    /**
     * Return the InputStream which belongs to the given key
     * 
     * @param key the key for which the InputStream get returned
     * @return in the InputStram associted with the given key
     * @throws IOException get thrown on IO error
     */
    protected InputStream getInputStream( final String key )
        throws IOException
    {
        // This was changed to SharedFileInputStream but reverted to 
        // fix JAMES-559. Usign SharedFileInputStream should be a good
        // performance improvement, but more checks have to be done
        // on the repository side to avoid concurrency in reading and
        // writing the same file.
        return new FileInputStream( encode( key ) );
    }

    /**
     * Return the OutputStream which belongs to the given key
     * 
     * @param key the key for which the OutputStream get returned
     * @return out the OutputStream
     * @throws IOException get thrown on IO error
     */
    protected OutputStream getOutputStream( final String key )
        throws IOException
    {
        return new FileOutputStream( getFile( key ) );
    }
    
    /**
     * Remove the object associated to the given key.
     *
     * @param key the key to remove
     */
    public synchronized void remove( final String key )
    {
        try
        {
            final File file = getFile( key );
            file.delete();
            if( DEBUG ) getLogger().debug( "removed key " + key );
        }
        catch( final Exception e )
        {
            throw new RuntimeException( "Exception caught while removing" +
                                        " an object: " + e );
        }
    }

    /**
     * 
     * Indicates if the given key is associated to a contained object
     * 
     * @param key the key which checked for
     * @return true if the repository contains the key
     */
    public synchronized boolean containsKey( final String key )
    {
        try
        {
            final File file = getFile( key );
            if( DEBUG ) getLogger().debug( "checking key " + key );
            return file.exists();
        }
        catch( final Exception e )
        {
            throw new RuntimeException( "Exception caught while searching " +
                                        "an object: " + e );
        }
    }

    /**
     * Returns the list of used keys.
     */
    public Iterator<String> list()
    {
        final File storeDir = new File( m_path );
        final String[] names = storeDir.list( m_filter );
        final List<String> list = new ArrayList<String>();

        for( int i = 0; i < names.length; i++ )
        {
            String decoded = decode(names[i]);
            list.add(decoded);
        }

        return list.iterator();
    }


    /**
     * Returns a String that uniquely identifies the object.
     * <b>Note:</b> since this method uses the Object.toString()
     * method, it's up to the caller to make sure that this method
     * doesn't change between different JVM executions (like
     * it may normally happen). For this reason, it's highly recommended
     * (even if not mandated) that Strings be used as keys.
     *
     * @param key the key for which the Object should be searched
     * @return result a unique String represent the Object which belongs to the key
     */
    protected String encode( final String key )
    {
        final byte[] bytes = key.getBytes();
        final char[] buffer = new char[ bytes.length << 1 ];

        for( int i = 0, j = 0; i < bytes.length; i++ )
        {
            final int k = bytes[ i ];
            buffer[ j++ ] = HEX_DIGITS[ ( k >>> 4 ) & BYTE_MASK ];
            buffer[ j++ ] = HEX_DIGITS[ k & BYTE_MASK ];
        }

        StringBuffer result = new StringBuffer();
        result.append( m_path );
        result.append( File.separator );
        result.append( buffer );
        result.append( m_extension );
        return result.toString();
    }


    /**
     * Inverse of encode exept it do not use path.
     * So decode(encode(s) - m_path) = s.
     * In other words it returns a String that can be used as key to retive
     * the record contained in the 'filename' file.
     *
     * @param filename the filename for which the key should generated
     * @return key a String which can be used to retrieve the filename
     */
    protected String decode( String filename )
    {
        filename = filename.substring( 0, filename.length() - m_extension.length() );
        final int size = filename.length();
        final byte[] bytes = new byte[ size >>> 1 ];

        for( int i = 0, j = 0; i < size; j++ )
        {
            bytes[ j ] = Byte.parseByte( filename.substring( i, i + 2 ), 16 );
            i +=2;
        }

        return new String( bytes );
    }
}
