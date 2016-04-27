/*******************************************************************************
 * Copyright (C) 2012 The University of Manchester
 *
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package org.apache.taverna.osgistarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.taverna.security.credentialmanager.CredentialManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import org.apache.taverna.osgilauncher.OsgiLauncher;
import org.apache.taverna.platform.run.api.RunService;
import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.apache.taverna.scufl2.api.io.WorkflowBundleReader;
import org.apache.taverna.scufl2.api.io.WorkflowBundleWriter;

/**
 *
 *
 * @author David Withers
 */
public class TavernaStarter {

	private static final String languageVersion = "0.15.1";
	private static final String osgiVersion = "0.2.1";
	private static final String engineVersion = "3.1.0";
	private static final String commonActivitiesVersion = "2.1.0";


	private static final String systemPackages =
			"org.apache.taverna.platform.execution.api;version=" + engineVersion + "," +
			"org.apache.taverna.platform.run.api;version="+ engineVersion + "," +
			"org.apache.taverna.configuration.app;version=" + engineVersion + "," +
			"org.apache.taverna.security.credentialmanager;version=" + engineVersion + "," +
			"org.apache.taverna.scufl2.api.activity;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.annotation;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.common;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.configurations;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.container;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.core;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.dispatchstack;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.io;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.io.structure;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.iterationstrategy;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.port;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.profiles;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.api.property;version="+languageVersion+"," +
			"org.apache.taverna.scufl2.ucfpackage;version="+languageVersion+"," +
			"org.apache.taverna.lang.observer;version=" + engineVersion + "," +
			"org.apache.taverna.platform.report;version=" + engineVersion + "," +
			"org.apache.log4j;version=1.2.16";

	private OsgiLauncher osgiLauncher;
	private BundleContext context;
	private RunService runService;
	private CredentialManager credentialManager;
	private WorkflowBundleIO workflowBundleIO;

	public TavernaStarter(File storageDirectory) throws IOException {
		URL bundleList = getClass().getClassLoader().getResource("config/taverna.osgi.bundles");
		BufferedReader bundleListReader = new BufferedReader(new InputStreamReader(
				bundleList.openStream()));
		String[] bundles = bundleListReader.readLine().split(",");
		List<URI> bundlesToInstall = new ArrayList<URI>();
		for (String bundle : bundles) {
			URL bundleURL = getClass().getClassLoader().getResource(bundle);
			try {
				bundlesToInstall.add(bundleURL.toURI());
			} catch (URISyntaxException ex) {
				throw new RuntimeException("Invalid URL from getResource(): " + bundleURL, ex);
			}
		}
		osgiLauncher = new OsgiLauncher(storageDirectory, bundlesToInstall);
		osgiLauncher.addBootDelegationPackages("org.xml.*,org.w3c.*");
		osgiLauncher.setCleanStorageDirectory(true);
		osgiLauncher.addSystemPackages(systemPackages);
	}


	public void start() throws BundleException {
		osgiLauncher.start();
		context = osgiLauncher.getContext();
		osgiLauncher.startServices(true);
	}

	public void stop() throws BundleException, InterruptedException {
		osgiLauncher.stop();
	}

	/**
	 * Returns the context.
	 *
	 * @return the context
	 */
	public BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the runService.
	 *
	 * @return the runService
	 */
	public RunService getRunService() {
		if (runService == null && context != null) {
			ServiceReference<RunService> serviceReference = context
					.getServiceReference(RunService.class);
			if (serviceReference == null) {
				System.out.println("Can't find RunService");
			} else {
				runService = context.getService(serviceReference);
			}
		}
		return runService;
	}

	/**
	 * Returns the credentialManager.
	 *
	 * @return the credentialManager
	 */
	public CredentialManager getCredentialManager() {
		if (credentialManager == null && context != null) {
			ServiceReference<CredentialManager> serviceReference = context
					.getServiceReference(CredentialManager.class);
			if (serviceReference == null) {
				System.out.println("Can't find CredentialManager");
			} else {
				credentialManager = context.getService(serviceReference);
			}
		}
		return credentialManager;
	}

	public WorkflowBundleIO getWorkflowBundleIO() {
		if (workflowBundleIO == null && context != null) {
			List<WorkflowBundleReader> workflowBundleReaders = new ArrayList<WorkflowBundleReader>();
			List<WorkflowBundleWriter> workflowBundleWriters = new ArrayList<WorkflowBundleWriter>();
			Collection<ServiceReference<WorkflowBundleReader>> readerServiceReferences = null;
			try {
				readerServiceReferences = context.getServiceReferences(WorkflowBundleReader.class, null);
			} catch (InvalidSyntaxException e) {
			}
			if (readerServiceReferences == null) {
				System.out.println("Can't find WorkflowBundleReaders");
			} else {
				for (ServiceReference<WorkflowBundleReader> serviceReference : readerServiceReferences) {
					workflowBundleReaders.add(context.getService(serviceReference));
				}
			}
			Collection<ServiceReference<WorkflowBundleWriter>> writerServiceReferences = null;
			try {
				writerServiceReferences = context.getServiceReferences(WorkflowBundleWriter.class, null);
			} catch (InvalidSyntaxException e) {
			}
			if (writerServiceReferences == null) {
				System.out.println("Can't find WorkflowBundleReaders");
			} else {
				for (ServiceReference<WorkflowBundleWriter> serviceReference : writerServiceReferences) {
					workflowBundleWriters.add(context.getService(serviceReference));
				}
			}
			workflowBundleIO = new WorkflowBundleIO();
			workflowBundleIO.setReaders(workflowBundleReaders);
			workflowBundleIO.setWriters(workflowBundleWriters);
		}
		return workflowBundleIO;
	}

}
