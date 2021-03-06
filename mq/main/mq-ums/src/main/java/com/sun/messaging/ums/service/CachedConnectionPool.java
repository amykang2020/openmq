/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.messaging.ums.service;

import com.sun.messaging.ums.common.Constants;
import com.sun.messaging.ums.factory.UMSConnectionFactory;
import com.sun.messaging.ums.resources.UMSResources;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.JMSException;

public class CachedConnectionPool implements Sweepable {

    //private String myName = null;
    private Properties props = null;
    
    //private com.sun.messaging.ConnectionFactory factory = null;
    
    //private String brokerAddress = null;
    private ArrayList<CachedConnection> connections =
            new ArrayList<CachedConnection>();
    private final ReentrantLock lock = new ReentrantLock();
    
    private Logger logger = UMSServiceImpl.logger;
       
    private String provider = null;
    
    private String serviceName = UMSServiceImpl.SERVICE_NAME;

    private static final String DEFAULT_CLASS_PREFIX = "com.sun.messaging.ums.provider.";
    
    private static final String DEFAULT_CLASS_POSTFIX = ".ProviderFactory";
    
    private String user = null;
    private transient String password = null;
    
    /**
     * UMSConnectionFactory is bound to a specific provider connection factory
     * based on the specified provider id.
     */
    private UMSConnectionFactory umsConnectionFactory = null;
    
    private Authenticator authenticator = null;
    
    public CachedConnectionPool(String provider, Properties props) throws JMSException {

        this.provider = provider;
        
        this.props = props;

        init();
        
    }
    
    /**
     * construct UMS connection factory
     * @throws javax.jms.JMSException
     */
    private void init() throws JMSException {
        
        try {
            
            String factoryClassName = null;
            
            //String defaultClassName = "com.sun.messaging.ums.provider." + provider + ".ProviderFactory";
            String defaultClassName = DEFAULT_CLASS_PREFIX + provider + DEFAULT_CLASS_POSTFIX;
            
            //check if defined in the <init-param> in web.xml    
            String propname = "mom." + provider + ".providerFactory";
                     
            //factoryClassName = props.getProperty(propname, defaultClassName);
            factoryClassName = props.getProperty(propname, defaultClassName);
           
            if (UMSServiceImpl.debug) {
                logger.info ("factory class name: " + factoryClassName);
            }
            
            umsConnectionFactory = (UMSConnectionFactory) Class.forName(factoryClassName).newInstance();
            
            umsConnectionFactory.init(props);
            
            //this is used to create UMS connections
            this.user = props.getProperty(Constants.IMQ_USER_NAME);
            
            //this is used to create UMS connections. NO base64 encoding
            //password encoding only required for app to UMS.
            //UMS to server uses provider internal encoding.
            this.password = props.getProperty(Constants.IMQ_USER_PASSWORD);
            
            
            //logger.info ("provider connection factory initialized ..., provider=" + provider);
            String msg = UMSResources.getResources().getKString(UMSResources.UMS_PROVIDER_INIT, this.provider);
            logger.info(msg);
            
            
            this.authenticator = new Authenticator (umsConnectionFactory, props);
            
            if (UMSServiceImpl.debug) {
                logger.info ("provider authenticator is ready to serve ..., provider=" + provider);
            }
            
        } catch (Exception e) {
            
            if (e instanceof JMSException ) {
                throw (JMSException) e;
            } else {
                JMSException jmse = new JMSException (e.getMessage());
                jmse.setLinkedException(e);
                
                throw jmse;
            }
        }
        
    }
    
    protected String authenticate (String user, String password) throws JMSException {
        return authenticator.authenticate(user, password);
    }
    
    protected void authenticateSid (String sid) throws JMSException {    
        authenticator.authenticateSid (sid);
    }
    
    public String nextSid () throws JMSException {
        return this.authenticator.nextSid();
    }
    
    //public boolean removeSid (String uuid) {
    //    return authenticator.removeSid(uuid);
    //}
    
    /**
     * Get ConnectionFactory.
     */
    public UMSConnectionFactory getUMSConnectionFactory () {
        return this.umsConnectionFactory;
    }

    /**
     * check out a cached connection.  This is called when a client
     * is created.
     * 
     * @return
     * @throws JMSException
     */
    public CachedConnection getCachedConnection() throws JMSException {

        CachedConnection cc = null;

        lock.lock();

        try {

            int size = connections.size();

            for (int index = 0; index < size; index++) {

                /**
                 * we can be smart and load-balance the clients
                 */
                if (connections.get(index).reachedMaxCapacity() == false) {
                    // get cached connection
                    cc = connections.get(index);
                    // exit loop
                    index = size;

                    logger.fine ("Got cached conn ...");
                }
            }

            if (cc == null) {

                // create new conn
                cc = this.newCachedConnection();

                if (UMSServiceImpl.debug) {
                    logger.info ("created new cc ..." + cc);
                }
            }

            /**
             * decrease permit by one.
             */
            cc.acquire();

        } finally {
            lock.unlock();
        }

        return cc;
    }

    /**
     * This is called when a client is sweeped/closed.
     * @param cc
     */
    public void releaseConnection(CachedConnection cc) {

        lock.lock();

        try {
            /**
             * increase permit by one. 
             */
            cc.release();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Only called from getCachedConnection and already acquired lock.
     * @return
     * @throws JMSException
     */
    private CachedConnection newCachedConnection() throws JMSException {

        Connection jmsConn = newJMSConnection();

        CachedConnection cachedConn = new CachedConnection(jmsConn, props);

        connections.add(cachedConn);
        
        if (UMSServiceImpl.debug) {
            logger.info ("XXX New jms/cc created, total=" + connections.size());
        }
        
        return cachedConn;
    }

    /**
     * Only called from newCachedConnection and already acquired lock.
     * @return
     * @throws JMSException
     */
    private Connection newJMSConnection() throws JMSException {
        // create connection
        //Connection conn = factory.createConnection(this.user, this.password);
        
        
        
        Connection conn = null;
        
        if (this.user == null) {
            conn = this.umsConnectionFactory.createConnection();
        } else {
            conn = this.umsConnectionFactory.createConnection(user, password);
        }
        
        // start connection
        conn.start();

        return conn;
    }

    public void sweep(long duration) {
        
        if (UMSServiceImpl.getDebug()) {
            logger.info ("sweeping  cached connection, duration (milli secs): " + duration);
        }
        
        Vector<CachedConnection> v = new Vector<CachedConnection>();

        long now = System.currentTimeMillis();

        int size = connections.size();

        for (int index = 0; index < size; index++) {

            CachedConnection cc = connections.get(index);

            if (UMSServiceImpl.debug) {
                logger.info ("Got cached connection: " + cc);
            }

            if (cc.inUse() == false) {

                long timestamp = cc.getTimestamp();

                if ((now - timestamp) > duration) {

                    // add to list
                    v.add(cc);
                    
                    if (UMSServiceImpl.debug) {
                        logger.info ("added connection to clean list: " + cc);
                    }
                }
            }

            Thread.yield();
        }

        if (v.size() > 0) {
            this.removeFromConnectionTable(v);
        } else {
            
            if (UMSServiceImpl.debug) {
                logger.fine ("No cc needs to be removed from cache ..., cache size: " + size);
            }
        }

    }

    public void close() {

        int size = this.connections.size();

        for (int index = 0; index < size; index++) {
            this.closeCachedConnection(connections.get(index));
        }

    }

    private void removeFromConnectionTable(List<CachedConnection> list) {

        int size = list.size();
        
        if (UMSServiceImpl.debug) {
            logger.info ("removing cached connection from pool, size=" + size);
        }
        
        for (int i = 0; i < size; i++) {

            CachedConnection cc = list.get(i);

            lock.lock();

            try {

                if (cc.inUse() == false) {
                    connections.remove(cc);
                }

            } finally {
                lock.unlock();
            }

            this.closeCachedConnection(cc);
            
            if (UMSServiceImpl.debug) {
                logger.info ("removed cached connection from pool: " + cc);
            }
        }
    }

    private void closeCachedConnection(CachedConnection cc) {
        try {
            cc.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
    
     public String toString() {
        return this.getClass().getName() + ", provider=" + this.provider + ", service=" + this.serviceName + ", #cc=" + this.connections.size();
    }
}
