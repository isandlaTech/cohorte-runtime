/**
 * File:   ISignalBroadcastProvider.java
 * Author: Thomas Calmant
 * Date:   23 sept. 2011
 */
package org.psem2m.isolates.services.remote.signals;

import org.psem2m.isolates.services.remote.signals.ISignalBroadcaster.EEmitterTargets;

/**
 * Represents a signal broadcast provider
 * 
 * @author Thomas Calmant
 */
public interface ISignalBroadcastProvider {

    /**
     * Sends the given data to the given targets.
     * 
     * @param aTargets
     *            Signal targets
     * @param aSignalName
     *            Signal name (URI like string)
     * @param aData
     *            Signal content (can't be null)
     * 
     * @throws UnsendableDataException
     *             The given data can't be sent using signals
     */
    void sendData(EEmitterTargets aTargets, String aSignalName, Object aData)
            throws UnsendableDataException;

    /**
     * Sends the given data to the given isolate
     * 
     * @param aIsolateId
     *            The target isolate ID
     * @param aSignalName
     *            Signal name (URI like string)
     * @param aData
     *            Signal content (can't be null)
     * 
     * @return True if the isolate ID exists, else false
     * 
     * @throws UnsendableDataException
     *             The given data can't be sent using signals
     */
    boolean sendData(String aIsolateId, String aSignalName, Object aData)
            throws UnsendableDataException;
}