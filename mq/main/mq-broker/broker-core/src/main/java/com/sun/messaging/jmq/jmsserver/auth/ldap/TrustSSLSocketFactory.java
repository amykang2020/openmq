/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * @(#)TrustSSLSocketFactory.java	1.8 06/28/07
 */ 

package com.sun.messaging.jmq.jmsserver.auth.ldap;

import javax.net.*;
import javax.net.ssl.*;
import com.sun.messaging.jmq.jmsserver.Globals;
import com.sun.messaging.jmq.util.log.Logger;

public abstract class TrustSSLSocketFactory extends javax.net.ssl.SSLSocketFactory {

    //private Logger logger = Globals.getLogger();

    public static SocketFactory getDefault() {

        try {

        return getTrustSocketFactory();

        }
        catch (java.security.NoSuchAlgorithmException e) {
        Globals.getLogger().log(Logger.ERROR, e.getMessage(), e);
        }
        catch (java.security.KeyManagementException e) { 
        Globals.getLogger().log(Logger.ERROR, e.getMessage(), e);
        }

        return null;
    }

    private static SSLSocketFactory getTrustSocketFactory() 
                   throws java.security.NoSuchAlgorithmException,
                          java.security.KeyManagementException {

        SSLContext ctx = SSLContext.getInstance("TLS");
        TrustManager[] tm = new TrustManager[1];
        tm[0] = new X509TrustManager() {
/* Unused
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain) {
                return;
                }
*/
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String type) {
                return;
                }
/* Unused
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain) {
                return;
                }
*/
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String type) {
                return;
                }
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
                }
                };

        ctx.init(null, tm, null);
        return ctx.getSocketFactory();
    }
}
