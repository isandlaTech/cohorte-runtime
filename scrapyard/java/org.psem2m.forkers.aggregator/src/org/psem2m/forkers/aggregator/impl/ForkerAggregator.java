/**
 * File:   ForkerAggregator.java
 * Author: Thomas Calmant
 * Date:   25 mai 2012
 */
package org.psem2m.forkers.aggregator.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.cohorte.monitor.api.IForkerAggregator;
import org.cohorte.monitor.api.IForkerPresenceListener;
import org.psem2m.forker.IForker;
import org.psem2m.forker.IForkerEventListener;
import org.psem2m.forker.IForkerStatus;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.constants.IPlatformProperties;
import org.psem2m.isolates.constants.ISignalsConstants;
import org.psem2m.isolates.services.conf.ISvcConfig;
import org.psem2m.isolates.services.dirs.IPlatformDirsSvc;
import org.psem2m.signals.HostAccess;
import org.psem2m.signals.ISignalBroadcaster;
import org.psem2m.signals.ISignalDirectory;
import org.psem2m.signals.ISignalDirectory.EBaseGroup;
import org.psem2m.signals.ISignalDirectoryConstants;
import org.psem2m.signals.ISignalReceiver;
import org.psem2m.signals.ISignalSendResult;

/**
 * The forker aggregator
 * 
 * Provides the forker interface and sends signals to real forkers to start/stop
 * isolates.
 * 
 * @author Thomas Calmant
 */
@Component(name = "psem2m-forker-aggregator-factory", publicFactory = false)
@Provides(specifications = IForkerAggregator.class)
@Instantiate(name = "psem2m-forker-aggregator")
public class ForkerAggregator implements IForkerAggregator, IPacketListener,
        Runnable {

    /** Maximum time without forker notification : 5 seconds */
    private static final long FORKER_TTL = 10000;

    /** ID of the forker events listeners */
    private static final String IPOJO_ID_LISTENERS = "forker-events-listeners";

    /** UDP Packet: Forker heart beat */
    private static final byte PACKET_FORKER_HEARTBEAT = 1;

    /** The configuration service */
    @Requires
    private ISvcConfig pConfig;

    /** The isolates directory */
    @Requires
    private ISignalDirectory pDirectory;

    /** Fixed thread pool for event listeners execution */
    private ExecutorService pEventExecutor;

    /** Forkers ID -&gt; Last seen time (LST) */
    private final Map<String, Long> pForkersLST = new HashMap<String, Long>();

    /** Isolate -&gt; Associated forker map */
    private final Map<String, String> pIsolateForkers = new HashMap<String, String>();

    /** The forker events listeners */
    @Requires(id = IPOJO_ID_LISTENERS, optional = true)
    private IForkerPresenceListener pListeners[];

    /** The logger */
    @Requires
    private IIsolateLoggerSvc pLogger;

    /** The multicast receiver */
    private MulticastReceiver pMulticast;

    /** Platform information service */
    @Requires
    private IPlatformDirsSvc pPlatform;

    /** The signal receiver, it must be online to retrieve its access point */
    @Requires(filter = "(" + ISignalReceiver.PROPERTY_ONLINE + "=true)")
    private ISignalReceiver pReceiver;

    /** Order results */
    private final Map<Integer, Integer> pResults = new HashMap<Integer, Integer>();

    /** The signal sender */
    @Requires
    private ISignalBroadcaster pSender;

    /** The TTL thread */
    private Thread pThread;

    /** The thread stopper */
    private boolean pThreadRunning = false;

    /**
     * Called by iPOJO when a {@link IForkerEventListener} service is bound.
     * 
     * Notifies the new listener of all known forkers.
     * 
     * @param aListener
     *            An event listener
     */
    @Bind(id = IPOJO_ID_LISTENERS)
    protected synchronized void bindListener(
            final IForkerPresenceListener aListener) {

        // Get all forkers
        final String[] forkers = pDirectory.getAllIsolates(
                IPlatformProperties.SPECIAL_ISOLATE_ID_FORKER, true, true);

        if (forkers != null) {
            // Call back the listener to register all of them
            for (final String forker : forkers) {

                aListener
                        .forkerReady(forker, pDirectory.getIsolateNode(forker));
            }
        }
    }

    /**
     * Extracts a string from the given buffer
     * 
     * @param aBuffer
     *            A bytes buffer
     * @return The read string, or null
     */
    protected String extractString(final ByteBuffer aBuffer) {

        // Get the length
        final int length = aBuffer.getShort();

        // Get the bytes
        final byte[] buffer = new byte[length];

        try {
            aBuffer.get(buffer);

        } catch (final BufferUnderflowException e) {
            pLogger.logSevere(this, "extractString", "Missing data:", e);
            return null;
        }

        // Return the string form
        try {
            return new String(buffer, "UTF-8");

        } catch (final UnsupportedEncodingException ex) {
            pLogger.logSevere(this, "extractString", "Unknown encoding:", ex);
            return null;
        }
    }

    /**
     * Notifies listeners of a forker event
     * 
     * @param aUID
     *            The forker UID
     * @param aNode
     *            The forker node name
     * @param aRegistered
     *            If true, the forker has been registered, else it has been lost
     */
    protected synchronized void fireForkerEvent(final String aUID,
            final String aNode, final boolean aRegistered) {

        // Copy the active listeners
        final IForkerPresenceListener[] listeners = Arrays.copyOf(pListeners,
                pListeners.length);

        // Call the listeners in another thread
        pEventExecutor.submit(new Runnable() {

            @Override
            public void run() {

                for (final IForkerPresenceListener listener : listeners) {
                    try {
                        if (aRegistered) {
                            listener.forkerReady(aUID, aNode);
                        } else {
                            listener.forkerLost(aUID, aNode);
                        }

                    } catch (final Exception e) {
                        // A listener failed
                        pLogger.logSevere(this, "fireForkerEvent",
                                "A forker event listener failed:\n", e);
                    }
                }
            }
        });
    }

    /**
     * Finds the first isolate with a forker ID on the given node
     * 
     * @param aNode
     *            The name of a node
     * @param aKind
     *            The kind the forker must handle
     * @return The first forker found on the node, or null
     */
    protected String getForker(final String aNode, final String aKind) {

        // Get all forkers
        final String[] forkers = pDirectory
                .getNameUIDs(IPlatformProperties.SPECIAL_NAME_FORKER);
        if (forkers == null || forkers.length == 0) {
            // No forker known (yet)
            return null;
        }

        if (aNode == null) {
            // No node given, use the first known forker
            return forkers[0];
        }

        for (final String forker : forkers) {

            if (aNode.equals(pDirectory.getIsolateNode(forker))) {
                // Matching forker
                pLogger.logDebug(this, "getForker", "FOUND=", forker);
                return forker;
            }
        }

        // No match found
        return null;
    }

    /**
     * Posts an order to the given forker and waits for the result. Returns
     * {@link IForker#REQUEST_TIMEOUT} if the time out expires before.
     * 
     * @param aForkerId
     *            ID of the forker to contact
     * @param aSignalName
     *            Signal name
     * @param aContent
     *            Signal content
     * @param aTimeout
     *            Maximum time to wait for an answer (in milliseconds)
     * @return The forker result (&ge;0) or an error code (&lt;0) (see
     *         {@link IForker})
     */
    protected int getForkerIntResult(final String aForkerId,
            final String aSignalName, final Object aContent, final long aTimeout) {

        // Send the order
        final Future<ISignalSendResult> waiter = pSender.post(aSignalName,
                aContent, aForkerId);

        try {
            // Wait a little...
            final ISignalSendResult result = waiter.get(aTimeout,
                    TimeUnit.MILLISECONDS);

            final Map<String, Object[]> results = result.getResults();
            if (results == null) {
                // No results at all
                pLogger.logWarn(this, "getForkerIntResult",
                        "No results from forker");
                return IForkerStatus.REQUEST_NO_RESULT;
            }

            final Object[] forkerResults = results.get(aForkerId);
            if (forkerResults == null || forkerResults.length != 1) {
                pLogger.logWarn(this, "getForkerIntResult",
                        "Unreadable result=", forkerResults);
                return IForkerStatus.REQUEST_NO_RESULT;
            }

            if (forkerResults[0] instanceof Number) {
                // Retrieve the forker result
                return ((Number) forkerResults[0]).intValue();

            } else {
                // Bad result
                pLogger.logWarn(this, "getForkerIntResult", "Invalid result=",
                        forkerResults[0]);
                return IForkerStatus.REQUEST_NO_RESULT;
            }

        } catch (final InterruptedException ex) {
            // Thread interrupted (end of the monitor ?), consider a time out
            pLogger.logDebug(this, "getForkerIntResult",
                    "Interrupted while waiting for an answer of forker=",
                    aForkerId, "sending signal=", aSignalName);

            return IForkerStatus.REQUEST_TIMEOUT;

        } catch (final TimeoutException e) {
            // Forker timed out
            pLogger.logWarn(this, "getForkerIntResult", "Forker=", aForkerId,
                    "timed out sending signal=", aSignalName);

            return IForkerStatus.REQUEST_TIMEOUT;

        } catch (final ExecutionException e) {
            // Error sending the request
            pLogger.logSevere(this, "getForkerIntResult",
                    "Error sending signal=", aSignalName, "to=", aForkerId,
                    ":", e);
            return IForkerStatus.REQUEST_ERROR;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.forkers.aggregator.impl.IPacketListener#handleError(java.lang
     * .Exception)
     */
    @Override
    public boolean handleError(final Exception aException) {

        pLogger.logWarn(this, "handleError",
                "Error while receiving a UDP packet:", aException);

        // Continue if the exception is not "important"
        return !(aException instanceof SocketException || aException instanceof NullPointerException);
    }

    /**
     * Handles a forker heart beat
     * 
     * @param aSenderAddress
     *            The packet sender address
     * @param aData
     *            The packet content decoder
     */
    protected void handleHeartBeat(final String aSenderAddress,
            final ByteBuffer aData) {

        /* Extract packet content */
        // ... the port (2 bytes)
        final int port = aData.getShort();

        // ... the application ID (string)
        final String applicationId = extractString(aData);

        // TODO: Check if the application corresponds to us
        if (!pConfig.getApplication().getApplicationId().equals(applicationId)) {
            // Not for us, ignore.
            // Avoid to log, as this will happen every heart beat
            // return;
        }

        // ... the isolate ID (string)
        final String forkerId = extractString(aData);

        // ... the node ID (string)
        final String nodeId = extractString(aData);

        // Registration flag
        final boolean needsRegistration;
        synchronized (pForkersLST) {
            // Test if it's a new forker
            needsRegistration = !pForkersLST.containsKey(forkerId);

            // Update the Last Seen Time
            pForkersLST.put(forkerId, System.currentTimeMillis());
        }

        if (needsRegistration) {
            // TODO: use pSender.postTo() to test the access

            // Register the forker in the internal directory
            pLogger.logDebug(this, "handleBeat", "Register forker=", forkerId);
            registerForker(forkerId, nodeId, aSenderAddress, port);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.forkers.aggregator.impl.IPacketListener#handlePacket(java.
     * net.DatagramPacket)
     */
    @Override
    public void handlePacket(final DatagramPacket aPacket) {

        // Get the content
        final byte[] data = aPacket.getData();

        // Make a little endian byte array reader, to extract the packet content
        final ByteBuffer buffer = ByteBuffer.wrap(data, 0, data.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        final byte packetType = buffer.get();
        switch (packetType) {
        case PACKET_FORKER_HEARTBEAT:
            handleHeartBeat(aPacket.getAddress().getHostAddress(), buffer);
            break;

        default:
            pLogger.logInfo(this, "handlePacket", "Unknown packet type=",
                    packetType);
            break;
        }
    }

    /**
     * Component invalidation
     */
    @Invalidate
    public void invalidate() {

        // Stop the event thread
        pEventExecutor.shutdownNow();
        pEventExecutor = null;

        // Clear all collections
        pForkersLST.clear();
        pResults.clear();
        pIsolateForkers.clear();

        // Stop the multicast listener
        if (pMulticast != null) {
            try {
                pMulticast.stop();

            } catch (final IOException ex) {
                pLogger.logWarn(this, "invalidate",
                        "Error stopping the multicast listener:", ex);
            }

            pMulticast = null;
        }

        // Wait a second for the thread to stop
        pThreadRunning = false;
        try {
            pThread.join(1000);

        } catch (final InterruptedException e) {
            // Join interrupted
        }

        if (pThread.isAlive()) {
            // Force interruption if necessary
            pThread.interrupt();
        }
        pThread = null;

        pLogger.logInfo(this, "invalidate", "Forker Aggregator invalidated");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cohorte.monitor.IForkerAggregator#isAlive(java.lang.String)
     */
    @Override
    public boolean isAlive(final String aUID) {

        final String forker = pIsolateForkers.get(aUID);
        if (forker == null) {
            pLogger.logSevere(this, "ping", "No forker known for isolate UID=",
                    aUID);
            return false;
        }

        // Prepare the order
        final Map<String, Object> order = new HashMap<String, Object>();
        order.put("uid", aUID);

        return getForkerIntResult(forker, IForkerOrders.SIGNAL_PING_ISOLATE,
                order, 1000) == 0;
    }

    /**
     * Registers a forker in the internal directory, using a heart beat signal
     * data, and notifies listeners on success.
     * 
     * @param aUID
     *            The forker UID
     * @param aNode
     *            The node hosting the forker
     * @param aHost
     *            The node host address
     * @param aPort
     *            The forker signals access port
     */
    protected void registerForker(final String aUID, final String aNode,
            final String aHost, final int aPort) {

        // Update the node host
        if (!pPlatform.getIsolateNode().equals(aNode)) {
            // Don't update our node
            pDirectory.setNodeAddress(aNode, aHost);
        }

        // Register the forker (it can already be in the directory)
        if (pDirectory.registerIsolate(aUID,
                IPlatformProperties.SPECIAL_ISOLATE_ID_FORKER, aNode, aPort)) {
            // Send it a SYN-ACK
            pDirectory.synchronizingIsolatePresence(aUID);
            pSender.fire(ISignalDirectoryConstants.SIGNAL_REGISTER_SYNACK,
                    null, aUID);

            // Fresh forker: we can send it a contact signal as someone else
            // may not known it
            sendContactSignal(aHost, aPort);
        }

        pLogger.logInfo(this, "registerForker", "Registered forker ID=", aUID,
                "Node=", aNode, "Port=", aPort);

        // Notify listeners
        fireForkerEvent(aUID, aNode, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        final List<String> toDelete = new ArrayList<String>();

        while (pThreadRunning) {

            synchronized (pForkersLST) {
                final long currentTime = System.currentTimeMillis();

                for (final Entry<String, Long> entry : pForkersLST.entrySet()) {
                    // First loop to detect forkers to delete
                    final String forkerId = entry.getKey();
                    final Long lastSeen = entry.getValue();

                    if (lastSeen == null) {
                        // Invalid entry, ignore it
                        pLogger.logWarn(this, "run",
                                "Found a null 'last seen' value for forker=",
                                forkerId);

                    } else if (currentTime - lastSeen > FORKER_TTL) {
                        // TTL reached
                        toDelete.add(forkerId);

                        pLogger.logInfo(this, "run", "Forker=", forkerId,
                                "reached TTL ->", (currentTime - lastSeen),
                                "ms");
                    }
                }

                for (final String forkerId : toDelete) {
                    // Unregister the forker
                    unregisterForker(forkerId);
                }
            }

            toDelete.clear();

            try {
                Thread.sleep(1000);

            } catch (final InterruptedException e) {
                // Interrupted
                return;
            }
        }
    }

    /**
     * Sends a CONTACT signal to the given access point.
     * 
     * @param aHost
     *            A host address
     * @param aPort
     *            A signal access port
     */
    protected void sendContactSignal(final String aHost, final int aPort) {

        try {
            // Set up the signal content
            final Map<String, Object> content = new HashMap<String, Object>();

            // Local access port
            content.put("port", pReceiver.getAccessInfo().getPort());

            // Send the signal
            final Object[] results = pSender.sendTo(
                    ISignalDirectoryConstants.SIGNAL_CONTACT, content,
                    new HostAccess(aHost, aPort));
            if (results == null) {
                // No response...
                pLogger.logWarn(this, "sendContactSignal",
                        "No response from host=", aHost, "port=", aPort);
            }

        } catch (final Exception e) {
            // Log...
            pLogger.logWarn(this, "sendContactSignal",
                    "Error sending the contact signal to host=", aHost,
                    "port=", aPort, ":", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cohorte.monitor.api.IForkerAggregator#setPlatformStopping()
     */
    @Override
    public void setPlatformStopping() {

        pSender.fireGroup(IForkerOrders.SIGNAL_PLATFORM_STOPPING, null,
                EBaseGroup.FORKERS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cohorte.monitor.api.IForkerAggregator#startIsolate(java.lang.String,
     * java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public int startIsolate(final String aUID, final String aNode,
            final String aKind, final Map<String, Object> aIsolateConfiguration) {

        final String forker = getForker(aNode, aKind);
        if (forker == null) {
            pLogger.logSevere(this, "startIsolate",
                    "No forker known for host=", aNode);
            return IForkerStatus.NO_MATCHING_FORKER;
        }

        // Prepare the order
        final Map<String, Object> order = new HashMap<String, Object>();
        order.put("isolateDescr", aIsolateConfiguration);

        // Associate the isolate to the found forker
        pIsolateForkers.put(aUID, forker);

        return getForkerIntResult(forker, IForkerOrders.SIGNAL_START_ISOLATE,
                order, 1000);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cohorte.monitor.IForkerAggregator#stopIsolate(java.lang.String)
     */
    @Override
    public boolean stopIsolate(final String aUID) {

        final String forker = pIsolateForkers.get(aUID);
        if (forker == null) {
            pLogger.logSevere(this, "stopIsolate",
                    "No forker known for isolate UID=", aUID);
            return false;
        }

        // Prepare the order
        final Map<String, Object> order = new HashMap<String, Object>();
        order.put("uid", aUID);

        // Send the order (don't care about the result)
        pSender.fire(IForkerOrders.SIGNAL_STOP_ISOLATE, order, forker);

        // TODO: test the result of the signal
        return true;
    }

    /**
     * Unregisters the given forker and notifies listeners on success
     * 
     * @param aUID
     *            The UID of the forker to unregister
     */
    protected synchronized void unregisterForker(final String aUID) {

        // Get the forker host
        final String forkerNode = pDirectory.getIsolateNode(aUID);

        pLogger.logDebug(this, "unregisterForker", "Unregistering forker=",
                aUID, "for node=", forkerNode);

        if (pDirectory.unregisterIsolate(aUID)) {
            // Forker has been removed
            fireForkerEvent(aUID, forkerNode, false);
        }

        // Remove the references to the forker
        pForkersLST.remove(aUID);

        // Clean up corresponding isolates
        final String[] isolates = pDirectory.getIsolatesOnNode(forkerNode);
        if (isolates != null) {

            for (final String isolate : isolates) {
                // Forget this isolate
                pLogger.logDebug(this, "unregisterForker", "Forget isolate=",
                        isolate);

                pIsolateForkers.remove(isolate);
                pDirectory.unregisterIsolate(isolate);
            }
        }

        // Send the isolate lost signals
        pSender.fireGroup(ISignalsConstants.ISOLATE_LOST_SIGNAL, aUID,
                EBaseGroup.ALL);
        if (isolates != null) {
            for (final String isolate : isolates) {
                pSender.fireGroup(ISignalsConstants.ISOLATE_LOST_SIGNAL,
                        isolate, EBaseGroup.ALL);
            }
        }
    }

    /** Component validation */
    @Validate
    public void validate() {

        // Start the event thread
        pEventExecutor = Executors.newFixedThreadPool(1);

        // Start the UDP heart beat listener
        // Get the multicast group and port
        final int port = pConfig.getApplication().getMulticastPort();
        final SocketAddress group = new InetSocketAddress(pConfig
                .getApplication().getMulticastGroup(), port);

        // Create the multicast receiver
        try {
            pMulticast = new MulticastReceiver(this, group, port);
            pMulticast.start();

        } catch (final IOException ex) {

            pLogger.logSevere(this, "validate",
                    "Couldn't start the multicast receiver for group=", group,
                    ex);

            try {
                // Clean up
                pMulticast.stop();

            } catch (final IOException e) {
                // Ignore
                pLogger.logInfo(this, "validate",
                        "Couldn't clean up the multicast receiver for group=",
                        group, ex);
            }

            pMulticast = null;
            return;
        }

        // Start the TTL thread
        pThreadRunning = true;
        pThread = new Thread(this);
        pThread.start();

        pLogger.logInfo(this, "validate", "Forker Aggregator validated");
    }
}