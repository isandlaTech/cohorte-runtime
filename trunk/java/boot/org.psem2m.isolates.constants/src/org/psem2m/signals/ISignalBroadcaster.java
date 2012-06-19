/**
 * File:   ISignalBroadcaster.java
 * Author: Thomas Calmant
 * Date:   19 sept. 2011
 */
package org.psem2m.signals;

import java.util.concurrent.Future;

/**
 * Represents a signal broadcast service
 * 
 * @author Thomas Calmant
 */
public interface ISignalBroadcaster {

    /** Signals request mode ACK : at least one listener must be there */
    String MODE_ACK = "ack";

    /** Signals request mode FORGET : return immediately */
    String MODE_FORGET = "forget";

    /** Signals request mode SEND : wait for all listeners to return */
    String MODE_SEND = "send";

    /** If true, the broadcaster can send to other isolates */
    String PROPERTY_ONLINE = "signal-receiver-online";

    /**
     * Sends a signal to the given target, without waiting for the result.
     * 
     * Returns the list of successfully reached isolates, which may not have a
     * listener for this signal.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aIsolates
     *            A list of isolate IDs
     * @return The list of reached isolates, None if there is no isolate to send
     *         the signal to.
     */
    String[] fire(String aSignalName, Object aContent, String... aIsolates);

    /**
     * Sends a signal to the given target, without waiting for the result.
     * 
     * Returns the list of successfully reached isolates, which may not have a
     * listener for this signal.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aGroups
     *            A list of isolates groups names
     * @return The list of reached isolates, None if there is no isolate to send
     *         the signal to.
     */
    String[] fireGroup(String aSignalName, Object aContent, String... aGroups);

    /**
     * Sends a signal to the given target in a different thread. See
     * send(signal, content, isolate, isolates, groups) for more details.
     * 
     * The result is a future object, allowing to wait for and to retrieve the
     * result of the signal.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aIsolates
     *            A list of isolate IDs
     * @return A Future result, that will content the signal results
     */
    Future<ISignalSendResult> post(String aSignalName, Object aContent,
            String... aIsolates);

    /**
     * Sends a signal to the given target in a different thread. See
     * send(signal, content, isolate, isolates, groups) for more details.
     * 
     * The result is a future object, allowing to wait for and to retrieve the
     * result of the signal.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aGroups
     *            A list of isolates groups names
     * @return A Future result, that will content the signal results (reached
     *         isolates results and failed isolates)
     */
    Future<ISignalSendResult> postGroup(String aSignalName, Object aContent,
            String... aGroups);

    /**
     * Sends a signal to the given target.
     * 
     * The target can be either the ID of an isolate, a list of group of
     * isolates or a list of isolates.
     * 
     * The result is a map, with an entry for each reached isolate. The
     * associated result can be empty.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aIsolates
     *            A list of isolate IDs
     * @return The signal results (reached isolates results and failed isolates)
     */
    ISignalSendResult send(String aSignalName, Object aContent,
            String... aIsolates);

    /**
     * Sends a signal to the given target.
     * 
     * The target can be either the ID of an isolate, a list of group of
     * isolates or a list of isolates.
     * 
     * The result is a map, with an entry for each reached isolate. The
     * associated result can be empty.
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aGroups
     *            A list of isolates groups names
     * @return The signal results (reached isolates results and failed isolates)
     */
    ISignalSendResult sendGroup(String aSignalName, Object aContent,
            String... aGroups);

    /**
     * Sends a signal to the given end point
     * 
     * @param aSignalName
     *            The signal name
     * @param aContent
     *            The signal content
     * @param aHost
     *            Target host name or IP
     * @param aPort
     *            Target port
     * @return The signal result (null or the listeners results array)
     * @throws Exception
     *             Error sending the signal
     */
    Object[] sendTo(String aSignalName, Object aContent, String aHost, int aPort)
            throws Exception;
}
