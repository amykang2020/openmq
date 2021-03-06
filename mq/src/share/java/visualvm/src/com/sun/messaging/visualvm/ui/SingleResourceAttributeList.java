/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.messaging.visualvm.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.openide.util.Exceptions;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;

/**
 * Abstract superclass representing lists which display the attributes of a single resource
 * 
 * There are two columns, one showing attribute names and one showing the corresponding attribute values
 *
 */
@SuppressWarnings("serial")
public abstract class SingleResourceAttributeList extends MQAttributeList {
    
	public SingleResourceAttributeList(DataViewComponent dvc) {
		super(dvc);
	}

	abstract String getMBeanObjectName();
	
	// array of attribute names
	String[] attributeNames = null;
	
	// map whose key is an attribute name and whose corresponding value is the attribute description 
	Map<String, String> attributeDescriptions = null;
    
	@Override
	public void initTableModel() {
        if (getTableModel() == null) {
            MQResourceListTableModel tm = new MQResourceListTableModel() {

				@Override
                public List loadData() {
                    List list = new ArrayList();
                    AttributeList attrList = null;
                    try {
                    	ObjectName mbeanObjName = new ObjectName(getMBeanObjectName());
	                    if ((getMBeanServerConnection() == null) || (mbeanObjName == null)) {
	                        return null;
	                    }
                        attrList = getMBeanServerConnection().getAttributes(mbeanObjName, getAttributeNames(mbeanObjName));
                        for (Object object : attrList) {
							Attribute attr = (Attribute)object;
							if (!shouldExclude(attr)){
								list.add(attr);
							}
						}
                    } catch (InstanceNotFoundException ex) {
                        // we've probably lost connection to the broker
                        return new ArrayList();
                    } catch (ReflectionException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IOException ex) {
                        // we've probably lost connection to the broker
                        return new ArrayList();
                    } catch (MalformedObjectNameException ex) {
						Exceptions.printStackTrace(ex);
					} catch (NullPointerException ex) {
						Exceptions.printStackTrace(ex);
					}

                    return list;
                }

                @Override
                public Object getDataValueAt(List l, int row, int col) {
                    //String value = null;

                    try {
                    	ObjectName mbeanObjName = new ObjectName(getMBeanObjectName());
                    	if ((getMBeanServerConnection() == null) || (mbeanObjName == null)) {
                    		return null;
                    	}                    	
                    } catch (MalformedObjectNameException ex) {
                    	Exceptions.printStackTrace(ex);
					}

                    Attribute attr = (Attribute) l.get(row);

                    if (col == 0) {
                        return attr.getName();
                    } else if (col == 1) {
                        return attr.getValue();
                    } else {
                        return null;
                    }
                }

				@Override
				public void updateCharts() {
					updateRegisteredCharts();
				}
            };
            
            tm.setAttrsInColumn(false);
            setTableModel(tm);
        }
    }   
	    
	/**
	 * Return whether we want to exclude the specified attribute from the list
	 * 
	 * @param attr
	 * @return
	 */
    protected abstract boolean shouldExclude(Attribute attr);

	/**
     * Return the readable attributes of the specified MBean 
     * 
     * If an error occurs then an empty array is returned
     * 
     * @param objectName
     */
	private String[] getAttributeNames(ObjectName mbeanObjName) {
		
		if (attributeNames!=null){
			return attributeNames;
		}
				
		attributeNames = new String[0];
		attributeDescriptions = new HashMap<String, String>();
		try {
			MBeanServerConnection mbsc = getMBeanServerConnection();
			if (mbsc==null){
				return attributeNames;
			}
			MBeanInfo thisMBeanInfo = mbsc.getMBeanInfo(mbeanObjName);
			MBeanAttributeInfo[] thisMBeanAttributeInfo = thisMBeanInfo.getAttributes();
			ArrayList<String> readableAttributeNames = new ArrayList<String>();
			for (int j = 0; j < thisMBeanAttributeInfo.length; j++) {
				if (thisMBeanAttributeInfo[j].isReadable()){
					String thisAttributeName = thisMBeanAttributeInfo[j].getName();    
					String thisAttributeDescription = thisMBeanAttributeInfo[j].getDescription();
					readableAttributeNames.add(thisAttributeName);
					attributeDescriptions.put(thisAttributeName, thisAttributeDescription);
				}                           	
			}
			attributeNames = new String[readableAttributeNames.size()];
			for (int k = 0; k < attributeNames.length; k++) {
				attributeNames[k]=readableAttributeNames.get(k);
			}
		} catch (NullPointerException e) {
			Exceptions.printStackTrace(e);
		} catch (InstanceNotFoundException e) {
            // MBean not found, probably because there is no broker running in this JVM
			return new String[0];
		} catch (IntrospectionException e) {
			Exceptions.printStackTrace(e);
		} catch (ReflectionException e) {
			Exceptions.printStackTrace(e);
		} catch (IOException e) {
			// we can't connect to the broker: broker has probably terminated
			return new String[0];
		}
		return attributeNames;
	}    
	    
    @Override
    public void handleItemQuery(Object obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDescriptionForAttribute(String attributeName){
    	return attributeDescriptions.get(attributeName);
    }
    
}

