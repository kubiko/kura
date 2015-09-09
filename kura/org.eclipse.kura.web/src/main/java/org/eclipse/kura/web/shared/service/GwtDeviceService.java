/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.service;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("device")
public interface GwtDeviceService extends RemoteService 
{	
	public ListLoadResult<GwtGroupedNVPair> findDeviceConfiguration(GwtXSRFToken xsfrToken) throws GwtKuraException;

	public ListLoadResult<GwtGroupedNVPair> findBundles(GwtXSRFToken xsfrToken) throws GwtKuraException;

	public ListLoadResult<GwtGroupedNVPair> findThreads(GwtXSRFToken xsfrToken) throws GwtKuraException;

	public ListLoadResult<GwtGroupedNVPair> findSystemProperties(GwtXSRFToken xsfrToken) throws GwtKuraException; 
	
	public String executeCommand(GwtXSRFToken xsfrToken, String cmd, String pwd) throws GwtKuraException;
}
