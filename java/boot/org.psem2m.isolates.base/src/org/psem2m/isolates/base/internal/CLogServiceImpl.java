/**
 * Copyright 2014 isandlaTech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.psem2m.isolates.base.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Implementation of the OSGi log service, based on PSEM2M logging utilities
 *
 * @author Thomas Calmant
 */
public class CLogServiceImpl implements LogService {

    /** Default bundle for the log service entries */
    private final Bundle pDefaultLogBundle;

    /** Internal log handler */
    private final CLogInternal pLogInternal;

    /**
     * Sets up the logger service
     *
     * @param aLogInternal
     *            The underlying logger
     * @param aDefaultBundle
     *            Default bundle object when the log entries needs one
     */
    public CLogServiceImpl(final CLogInternal aLogInternal,
            final Bundle aDefaultBundle) {

        pLogInternal = aLogInternal;
        pDefaultLogBundle = aDefaultBundle;
        log(LogService.LOG_INFO,
                String.format(
                        "CLogServiceImpl.<init>: instanciated - SymbolicName=[%s] LogInternal=[%s]",
                        pDefaultLogBundle.getSymbolicName(), pLogInternal));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.log.LogService#log(int, java.lang.String)
     */
    @Override
    public void log(final int aLevel, final String aMessage) {

        pLogInternal.addEntry(new CLogEntry(null, pDefaultLogBundle, aLevel,
                aMessage, null));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.log.LogService#log(int, java.lang.String,
     * java.lang.Throwable)
     */
    @Override
    public void log(final int aLevel, final String aMessage,
            final Throwable aThrowable) {

        pLogInternal.addEntry(new CLogEntry(null, pDefaultLogBundle, aLevel,
                aMessage, aThrowable));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
     * int, java.lang.String)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void log(final ServiceReference aServiceReference, final int aLevel,
            final String aMessage) {

        pLogInternal.addEntry(new CLogEntry(aServiceReference,
                pDefaultLogBundle, aLevel, aMessage, null));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
     * int, java.lang.String, java.lang.Throwable)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void log(final ServiceReference aServiceReference, final int aLevel,
            final String aMessage, final Throwable aThrowable) {

        pLogInternal.addEntry(new CLogEntry(aServiceReference,
                pDefaultLogBundle, aLevel, aMessage, aThrowable));
    }
}
