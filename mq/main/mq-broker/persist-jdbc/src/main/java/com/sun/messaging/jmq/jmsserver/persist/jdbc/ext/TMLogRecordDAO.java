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

package com.sun.messaging.jmq.jmsserver.persist.jdbc.ext;

import java.util.List;
import java.sql.Connection;
import com.sun.messaging.jmq.jmsserver.persist.jdbc.comm.BaseDAO;
import com.sun.messaging.bridge.api.DupKeyException;
import com.sun.messaging.bridge.api.KeyNotFoundException;
import com.sun.messaging.bridge.api.UpdateOpaqueDataCallback;

/**
 * This class is an interface for XA transaction manager txlog
 * that is used by JMS Bridge 
 *
 * @author amyk
 */
public interface TMLogRecordDAO extends BaseDAO {

    /**
     * TMLogRecord table:
     * Holds all the txlog records
     *
     * CREATE TABLE MQTMLR??<schemaVersion>[C<clusterID>|S<brokerID>] (
     *     XID VARCHAR(256) NOT NULL,\
     *     LOG_RECORD VARBINARY(2048) NOT NULL,\
     *     NAME VARCHAR(100) NOT NULL,\
     *     BROKER_ID VARCHAR(100) NOT NULL,\
     *     CREATED_TS DOUBLE INTEGER NOT NULL,\
     *     UPDATED_TS DOUBLE INTEGER NOT NULL,\
     *     PRIMARY KEY(XID))
     *
     * XID - Global XID 
     * LOG_RECORD - Log record data
     * NAME - the jmsbridge name
     * BROKER_ID - The Broker ID
     * CREATED_TS_COLUMN - Timestamp when the record is created 
     * UPDATED_TS_COLUMN - Timestamp when the record was last updated 
     */
    public static final String XID_COLUMN = "XID";
    public static final String LOG_RECORD_COLUMN = "LOG_RECORD";
    public static final String NAME_COLUMN = "NAME";
    public static final String BROKER_ID_COLUMN = "BROKER_ID";
    public static final String CREATED_TS_COLUMN = "CREATED_TS";
    public static final String UPDATED_TS_COLUMN = "UPDATED_TS";

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param logRecord log record data
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @throws DupKeyException if already exist 
     *         else Exception on error
     */
    public void insert(Connection conn,
                       String xid, byte[] logRecord,
                       String name,
                       java.util.logging.Logger logger_)
                       throws DupKeyException, Exception;

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param logRecord log record data
     * @param name the jmsbridge name
     * @param callback to obtain updated data 
     * @param addIfNotExist
     * @param logger_ can be null;
     * @throws KeyNotFoundException if not found and addIfNotExist false
     *         else Exception on error
     */
    public void updateLogRecord(Connection conn,
                                String xid, byte[] logRecord, String name,
                                UpdateOpaqueDataCallback callback,
                                boolean addIfNotExist,
                                java.util.logging.Logger logger_)
                                throws KeyNotFoundException, Exception;

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @throws KeyNotFoundException if not found 
     *         else Exception on error
     */
    public void delete(Connection conn,
                       String xid, String name,
                       java.util.logging.Logger logger_)
                       throws KeyNotFoundException, Exception;
    /**
     * Delete all by jmsbridge name for this broker
     *
     * @param conn database connection
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @throws KeyNotFoundException if not found 
     *         else Exception on error
     */
    public void deleteAllByName(Connection conn,
                                String name,
                                java.util.logging.Logger logger_)
                                throws KeyNotFoundException, Exception;

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @return null if not found
     * @throws Exception
     */
    public byte[] getLogRecord(Connection conn, 
                               String xid, String name,
                               java.util.logging.Logger logger_)
                               throws Exception;

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @throws KeyNotFoundException if not found
     *         else Exception on error
     */
    public long getUpdatedTime(Connection conn, String xid, String name,
                               java.util.logging.Logger logger_)
                               throws KeyNotFoundException, Exception;

    /**
     * @param conn database connection
     * @param xid the global xid
     * @param name the jmsbridge name
     * @param logger_ can be null;
     * @throws KeyNotFoundException if not found
     *         else Exception on error
     */
    public long getCreatedTime(Connection conn, String xid, String name,
                               java.util.logging.Logger logger_)
                               throws KeyNotFoundException, Exception;

    /**
     * @param conn database connection
     * @param name the jmsbridge name
     * @param brokerID
     * @param logger_ can be null;
     * @return a list of log records 
     * @throws Exception
     */
    public List getLogRecordsByNameByBroker(Connection conn, String name,
                                            String brokerID, 
                                            java.util.logging.Logger logger_)
                                            throws Exception;

    /**
     * @param conn database connection
     * @param brokerID 
     * @param logger_ can be null;
     * @return a list of names in all log records owned by the brokerID 
     * @throws Exception
     */
    public List getNamesByBroker(Connection conn, String brokerID,
                                 java.util.logging.Logger logger_)
                                 throws Exception;


}
