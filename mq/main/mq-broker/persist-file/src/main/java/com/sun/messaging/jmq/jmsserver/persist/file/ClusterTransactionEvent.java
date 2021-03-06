/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.messaging.jmq.jmsserver.persist.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.sun.messaging.jmq.jmsserver.Globals;
import com.sun.messaging.jmq.jmsserver.data.BaseTransaction;
import com.sun.messaging.jmq.jmsserver.data.ClusterTransaction;
import com.sun.messaging.jmq.jmsserver.data.TransactionBroker;
import com.sun.messaging.jmq.jmsserver.data.TransactionState;
import com.sun.messaging.jmq.jmsserver.data.TransactionWork;
import com.sun.messaging.jmq.jmsserver.persist.api.Store;
import com.sun.messaging.jmq.jmsserver.util.BrokerException;
import com.sun.messaging.jmq.util.io.FilteringObjectInputStream;
import com.sun.messaging.jmq.util.log.Logger;

public abstract class ClusterTransactionEvent extends TransactionEvent {

	public static final byte Type2PPrepareEvent = 1;
	public static final byte Type2PCompleteEvent = 2;

	ClusterTransaction clusterTransaction;

	static TransactionEvent create(byte subtype) {
		TransactionEvent result = null;
		switch (subtype) {
		case Type2PPrepareEvent:
			result = new ClusterTransaction2PPrepareEvent();
			break;
		case Type2PCompleteEvent:
			result = new ClusterTransaction2PCompleteEvent();
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return result;
	}

	int getType() {
		return BaseTransaction.CLUSTER_TRANSACTION_TYPE;
	}
	abstract int getSubType();
}

class ClusterTransaction2PPrepareEvent extends ClusterTransactionEvent {
	int getSubType() {
		return Type2PPrepareEvent;
	}
	String getPrefix() {
		return "ClusterTransaction2PPrepareEvent: " + Thread.currentThread().getName();
	}
	public byte[] writeToBytes() throws IOException {
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + " writeToBytes");
		}
		// Log all msgs and acks for producing and consuming txn
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		dos.writeByte(BaseTransaction.CLUSTER_TRANSACTION_TYPE);
		dos.writeByte(Type2PPrepareEvent);
		
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + "post write types  size= "+ dos.size());
		}

		clusterTransaction.getTransactionDetails().writeContent(dos);
		
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + "post write content  size= "+ dos.size());
		}
		clusterTransaction.getTransactionWork().writeWork(dos);

		
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + "post write work  size= "+ dos.size());
		}
		
		ByteArrayOutputStream baos2 = new ByteArrayOutputStream(1024);
		ObjectOutputStream oos = new ObjectOutputStream(baos2);

		oos.writeObject(clusterTransaction.getTransactionState());
		oos.writeObject(clusterTransaction.getTransactionBrokers());
		oos.close();

		byte[] data = baos2.toByteArray();
		int length = data.length;
		dos.writeInt(length);
		dos.write(data);

		baos2.close();

		dos.close();
		baos.close();

		byte[] data2 = baos.toByteArray();
		return data2;

	}

	public void readFromBytes(byte[] data) throws IOException, BrokerException {
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + "readFromBytes");
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		clusterTransaction = new ClusterTransaction();
		dis.skip(2);
		

		clusterTransaction.getTransactionDetails().readContent(dis);
		if (Store.getDEBUG()) {
			Globals.getLogger().log(Logger.DEBUG,
					getPrefix() + "read details "+ clusterTransaction.getTransactionDetails());
		}
		TransactionWork work = new TransactionWork();
		work.readWork(dis);
		clusterTransaction.setTransactionWork(work);

		int objectBodySize = dis.readInt();

		byte[] objectBody = new byte[objectBodySize];
		dis.read(objectBody);

		ByteArrayInputStream bais2 = new ByteArrayInputStream(objectBody);
		ObjectInputStream ois = new FilteringObjectInputStream(bais2);

		try {

			clusterTransaction.setTransactionState((TransactionState) ois
					.readObject());
			clusterTransaction.setTransactionBrokers((TransactionBroker[]) ois
					.readObject());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		ois.close();
		bais2.close();

		dis.close();
		bais.close();
	}

}

class ClusterTransaction2PCompleteEvent extends ClusterTransactionEvent {
	
	int getSubType() {
		return Type2PCompleteEvent;
	}
	byte[] writeToBytes() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(BaseTransaction.CLUSTER_TRANSACTION_TYPE);
		dos.writeByte(Type2PCompleteEvent);
		clusterTransaction.getTransactionDetails().writeContent(dos);

		byte[] data = bos.toByteArray();
		dos.close();
		bos.close();
		return data;
	}

	void readFromBytes(byte[] data) throws IOException, BrokerException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);

		clusterTransaction = new ClusterTransaction();
		dis.skip(2);
		clusterTransaction.getTransactionDetails().readContent(dis);

		dis.close();
		bais.close();
	}
}
