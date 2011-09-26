/*******************************************************************************
 * Copyright (c) 2011 www.isandlatech.com (www.isandlatech.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ogattaz (isandlaTech) - initial API and implementation
 *******************************************************************************/
package org.psem2m.isolates.loggers.impl;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.osgi.framework.BundleException;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.base.activators.CPojoBase;
import org.psem2m.isolates.loggers.ILogChannelSvc;
import org.psem2m.isolates.loggers.ILogChannelsSvc;
import org.psem2m.utilities.logging.CLogLineBuffer;
import org.psem2m.utilities.logging.IActivityRequester;

/**
 * @author isandlatech (www.isandlatech.com) - ogattaz
 * 
 */
public class CLogChannelSvc extends CPojoBase implements ILogChannelSvc {

	/**
	 * field managed by iPojo (see metadata.xml)
	 * 
	 * <requires field="pChannelName" />
	 */
	private String pChannelName;

	/**
	 * Service reference managed by iPojo (see metadata.xml)
	 * 
	 * This service is the general logger of the current isolate
	 **/
	private IIsolateLoggerSvc pIsolateLoggerSvc;

	/**
	 * Service reference managed by iPojo (see metadata.xml)
	 * 
	 * This service is the general logger of the current isolate
	 **/
	private ILogChannelsSvc pLogChannelsSvc;

	/** **/
	private ILogChannelSvc pLogger;

	/**
	 * Explicit default constructor
	 */
	public CLogChannelSvc() {
		super();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLogger#close()
	 */
	@Override
	public void close() {
		pLogger.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.CXObjectBase#destroy()
	 */
	@Override
	public void destroy() {
		pLogger = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLogger#getRequester()
	 */
	@Override
	public IActivityRequester getRequester() {
		return pLogger.getRequester();
	}

	/**
	 * @return
	 */
	private boolean hasLogger() {
		return pLogger != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.isolates.base.CPojoBase#invalidatePojo()
	 */
	@Override
	public void invalidatePojo() throws BundleException {
		// logs in the isolate logger
		pIsolateLoggerSvc.logInfo(this, "invalidatePojo", "INVALIDATE",
				toDescription());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLoggerBase#isLogDebugOn()
	 */
	@Override
	public boolean isLogDebugOn() {
		return (!hasLogger()) ? false : pLogger.isLogDebugOn();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#isLoggable(java.util
	 * .logging.Level)
	 */
	@Override
	public boolean isLoggable(final Level aLevel) {
		return (!hasLogger()) ? false : pLogger.isLoggable(aLevel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLoggerBase#isLogInfoOn()
	 */
	@Override
	public boolean isLogInfoOn() {
		return (!hasLogger()) ? false : pLogger.isLogInfoOn();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLoggerBase#isLogSevereOn()
	 */
	@Override
	public boolean isLogSevereOn() {
		return (!hasLogger()) ? false : pLogger.isLogSevereOn();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLoggerBase#isLogWarningOn()
	 */
	@Override
	public boolean isLogWarningOn() {
		return (!hasLogger()) ? false : pLogger.isLogWarningOn();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#log(java.util.logging
	 * .Level, java.lang.Object, java.lang.CharSequence, java.lang.Object[])
	 */
	@Override
	public void log(final Level aLevel, final Object aWho,
			final CharSequence aWhat, final Object... aInfos) {
		if (hasLogger()) {
			pLogger.log(aLevel, aWho, aWhat, aInfos);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#log(java.util.logging
	 * .LogRecord)
	 */
	@Override
	public void log(final LogRecord record) {
		if (hasLogger()) {
			pLogger.log(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#logDebug(java.lang.Object
	 * , java.lang.CharSequence, java.lang.Object[])
	 */
	@Override
	public void logDebug(final Object aWho, final CharSequence aWhat,
			final Object... aInfos) {
		if (hasLogger()) {
			pLogger.logDebug(aWho, aWhat, aInfos);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#logInfo(java.lang.Object
	 * , java.lang.CharSequence, java.lang.Object[])
	 */
	@Override
	public void logInfo(final Object aWho, final CharSequence aWhat,
			final Object... aInfos) {
		if (hasLogger()) {
			pLogger.logInfo(aWho, aWhat, aInfos);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#logSevere(java.lang.
	 * Object, java.lang.CharSequence, java.lang.Object[])
	 */
	@Override
	public void logSevere(final Object aWho, final CharSequence aWhat,
			final Object... aInfos) {
		if (hasLogger()) {
			pLogger.logSevere(aWho, aWhat, aInfos);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLoggerBase#logWarn(java.lang.Object
	 * , java.lang.CharSequence, java.lang.Object[])
	 */
	@Override
	public void logWarn(final Object aWho, final CharSequence aWhat,
			final Object... aInfos) {
		if (hasLogger()) {
			pLogger.logWarn(aWho, aWhat, aInfos);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.logging.IActivityLogger#popLogLineBuffer()
	 */
	@Override
	public CLogLineBuffer popLogLineBuffer() {
		return (!hasLogger()) ? null : pLogger.popLogLineBuffer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.psem2m.utilities.logging.IActivityLogger#pushLogLineBuffer(org.psem2m
	 * .utilities.logging.CLogLineBuffer)
	 */
	@Override
	public void pushLogLineBuffer(final CLogLineBuffer aLoggerLineBuffer) {
		if (hasLogger()) {
			pLogger.pushLogLineBuffer(aLoggerLineBuffer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.isolates.base.CPojoBase#validatePojo()
	 */
	@Override
	public void validatePojo() throws BundleException {
		// logs in the isolate logger
		pIsolateLoggerSvc.logInfo(this, "validatePojo", "VALIDATE",
				toDescription());

		try {
			pLogger = pLogChannelsSvc.getLogChannel(pChannelName);
		} catch (Exception e) {
			pIsolateLoggerSvc.logSevere(this, "validatePojo", e);
			pLogger = null;
		}

	}
}
