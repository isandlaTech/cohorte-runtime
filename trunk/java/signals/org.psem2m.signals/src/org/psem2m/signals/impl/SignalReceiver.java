/**
 * File:   SignalReceiver.java
 * Author: Thomas Calmant
 * Date:   23 sept. 2011
 */
package org.psem2m.signals.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleException;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.base.Utilities;
import org.psem2m.isolates.base.activators.CPojoBase;
import org.psem2m.signals.HostAccess;
import org.psem2m.signals.ISignalBroadcaster;
import org.psem2m.signals.ISignalData;
import org.psem2m.signals.ISignalListener;
import org.psem2m.signals.ISignalReceiver;
import org.psem2m.signals.ISignalReceptionProvider;
import org.psem2m.signals.SignalResult;

/**
 * Base signal receiver logic
 * 
 * @author Thomas Calmant
 */
@Component(name = "psem2m-signal-receiver-factory", publicFactory = false)
@Provides(specifications = ISignalReceiver.class)
@Instantiate(name = "psem2m-signal-receiver")
public class SignalReceiver extends CPojoBase implements ISignalReceiver {

    /** Receivers dependency ID */
    private static final String ID_RECEIVERS = "receivers";

    /** Signal listeners */
    private final Map<String, Set<ISignalListener>> pListeners = new HashMap<String, Set<ISignalListener>>();

    /** Log service */
    @Requires
    private IIsolateLoggerSvc pLogger;

    /** Number of available providers */
    private int pNbProviders = 0;

    /** Notification thread pool */
    private ExecutorService pNotificationExecutor;

    /** On-line service property */
    @ServiceProperty(name = ISignalReceiver.PROPERTY_ONLINE, value = "false", mandatory = true)
    private boolean pPropertyOnline;

    /** Reception providers */
    @Requires(id = ID_RECEIVERS, optional = true)
    private ISignalReceptionProvider[] pReceivers;

    /**
     * Method called by iPOJO when a reception provider is bound
     * 
     * @param aProvider
     *            The new provider
     */
    @Bind(id = ID_RECEIVERS, aggregate = true, filter = "("
            + ISignalReceptionProvider.PROPERTY_READY + "=true)")
    protected void bindProvider(final ISignalReceptionProvider aProvider) {

        // Register to the provider
        if (!aProvider.setReceiver(this)) {
            pLogger.logInfo(this, "bindProvider",
                    "Error registering to the reception provider=", aProvider);
        }

        // Increase the number of available providers
        pNbProviders++;

        // We're now on-line
        pPropertyOnline = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.impl.ISignalReceiver#getAccessInfo()
     */
    @Override
    public HostAccess getAccessInfo() {

        for (final ISignalReceptionProvider receiver : pReceivers) {

            final HostAccess access = receiver.getAccessInfo();
            if (access != null) {
                // Return the first one
                return access;
            }
        }

        // No valid access found
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.signals.ISignalReceiver#handleReceivedSignal(java.lang.String,
     * org.psem2m.signals.ISignalData, java.lang.String)
     */
    @Override
    public SignalResult handleReceivedSignal(final String aSignalName,
            final ISignalData aSignalData, final String aMode) {

        if (ISignalBroadcaster.MODE_SEND.equalsIgnoreCase(aMode)) {
            // Standard mode
            return notifyListeners(aSignalName, aSignalData);

        } else if (ISignalBroadcaster.MODE_FORGET.equalsIgnoreCase(aMode)) {
            /*
             * Signal V1 mode. Fire and forget mode : the client doesn't want a
             * result -> Start a thread to notify listener and return
             * immediately
             */
            notifyListenersInThread(aSignalName, aSignalData);
            return new SignalResult(200, "Signal thread started");

        } else if (ISignalBroadcaster.MODE_ACK.equalsIgnoreCase(aMode)) {

            SignalResult result = null;

            // Test if at least one listener will be notified
            synchronized (pListeners) {
                for (final String signal : pListeners.keySet()) {
                    // Take care of jokers ('*' and '?')
                    if (Utilities.matchFilter(aSignalName, signal)) {
                        result = new SignalResult(200,
                                "At least one listener found");
                        break;
                    }
                }
            }

            if (result != null) {
                /*
                 * Start the treatment in another thread, as at least one
                 * listener was found
                 */
                notifyListenersInThread(aSignalName, aSignalData);
                return result;

            } else {
                // No listener found
                return new SignalResult(404, "No listener found for "
                        + aSignalName);
            }
        }

        // Unknown mode
        return new SignalResult(501, "Unknown mode '" + aMode + "'");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#invalidatePojo()
     */
    @Override
    @Invalidate
    public void invalidatePojo() throws BundleException {

        // Unregister from all providers
        for (final ISignalReceptionProvider provider : pReceivers) {
            provider.unsetReceiver(this);
        }

        // Clear listeners
        synchronized (pListeners) {
            pListeners.clear();
        }

        // Stop the executor
        pNotificationExecutor.shutdown();

        pLogger.logInfo(this, "invalidatePojo", "Base Signal Receiver Gone");
    }

    /**
     * Tests if the receiver is on line.
     * 
     * @return True if the receiver is on-line
     */
    public boolean isOnline() {

        return pPropertyOnline;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.ISignalReceiver#localReception(java.lang.String,
     * org.psem2m.signals.ISignalData, java.lang.String)
     */
    @Override
    public SignalResult localReception(final String aSignalName,
            final ISignalData aData, final String aMode) {

        // Simulate a normal reception
        return handleReceivedSignal(aSignalName, aData, aMode);
    }

    /**
     * Listeners notification loop
     * 
     * @param aSignalName
     *            A signal name
     * @param aSignalData
     *            The signal data
     * @return A (code, listeners results) couple
     */
    protected SignalResult notifyListeners(final String aSignalName,
            final ISignalData aSignalData) {

        final Set<ISignalListener> signalListeners = new HashSet<ISignalListener>();

        // Get listeners set
        synchronized (pListeners) {

            for (final String signal : pListeners.keySet()) {
                // Take care of jokers ('*' and '?')
                if (Utilities.matchFilter(aSignalName, signal)) {
                    signalListeners.addAll(pListeners.get(signal));
                }
            }
        }

        // Notify listeners
        final List<Object> results = new ArrayList<Object>(pListeners.size());
        for (final ISignalListener listener : signalListeners) {

            try {
                final Object result = listener.handleReceivedSignal(
                        aSignalName, aSignalData);
                if (result != null) {
                    results.add(result);
                }

            } catch (final Throwable throwable) {
                pLogger.logWarn(this, "handleReceivedSignal",
                        "Error notifying a signal listener", throwable);
            }
        }

        return new SignalResult(200, results);
    }

    /**
     * Starts a new thread to call {@link #notifyListeners(String, ISignalData)}
     * 
     * @param aSignalName
     *            A signal name
     * @param aSignalData
     *            The signal data
     */
    private void notifyListenersInThread(final String aSignalName,
            final ISignalData aSignalData) {

        new Thread(new Runnable() {

            @Override
            public void run() {

                // Don't care about the result
                notifyListeners(aSignalName, aSignalData);
            }
        }).start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.impl.ISignalReceiver#registerListener
     * (java.lang.String, org.psem2m.signals.impl.ISignalListener)
     */
    @Override
    public void registerListener(final String aSignalName,
            final ISignalListener aListener) {

        if (aSignalName == null || aSignalName.isEmpty() || aListener == null) {
            // Invalid call
            return;
        }

        synchronized (pListeners) {

            // Get or create the signal listeners set
            Set<ISignalListener> signalListeners = pListeners.get(aSignalName);

            if (signalListeners == null) {
                signalListeners = new LinkedHashSet<ISignalListener>();
                pListeners.put(aSignalName, signalListeners);
            }

            synchronized (signalListeners) {
                signalListeners.add(aListener);
            }
        }
    }

    /**
     * Called by iPOJO when a reception provider is gone
     * 
     * @param aProvider
     *            A reception provider service
     */
    @Unbind(id = ID_RECEIVERS, aggregate = true)
    protected void unbindProvider(final ISignalReceptionProvider aProvider) {

        aProvider.unsetReceiver(this);

        // Decrease the number of available providers
        pNbProviders--;

        if (pNbProviders == 0) {
            // No more provider, we're not on-line anymore
            pPropertyOnline = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.impl.ISignalReceiver#
     * unregisterListener(java.lang.String,
     * org.psem2m.signals.impl.ISignalListener)
     */
    @Override
    public void unregisterListener(final String aSignalName,
            final ISignalListener aListener) {

        if (aSignalName == null || aSignalName.isEmpty() || aListener == null) {
            // Invalid call
            return;
        }

        synchronized (pListeners) {

            // Get or create the signal listeners set
            final Set<ISignalListener> signalListeners = pListeners
                    .get(aSignalName);

            if (signalListeners != null) {
                synchronized (signalListeners) {
                    signalListeners.remove(aListener);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#validatePojo()
     */
    @Override
    @Validate
    public void validatePojo() throws BundleException {

        // Set up the thread pool
        pNotificationExecutor = Executors.newCachedThreadPool();

        pLogger.logInfo(this, "validatePojo", "Base Signal Receiver Ready");
    }
}
