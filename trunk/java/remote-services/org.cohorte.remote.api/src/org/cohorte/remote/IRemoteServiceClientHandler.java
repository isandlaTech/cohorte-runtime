/**
 * File:   IRemoteServiceClientHandler.java
 * Author: Thomas Calmant
 * Date:   27 juil. 2011
 */
package org.cohorte.remote;

import java.util.Collection;

import org.cohorte.remote.beans.RemoteServiceRegistration;
import org.osgi.framework.Bundle;

/**
 * Represents a client-side remote services handler
 * 
 * @author Thomas Calmant
 */
public interface IRemoteServiceClientHandler {

    /**
     * Returns the list of services which proxy must be updated due to the
     * bundle event (new classes, ...)
     * 
     * @param aBundle
     *            The bundle that has been started
     * @return A list of service IDs that must be updated
     */
    Collection<String> bundleStarted(final Bundle aBundle);

    /**
     * Returns the list of services which proxy must be updated due to the
     * bundle event (missing classes, ...)
     * 
     * @param aBundle
     *            The bundle that has been stopped
     * @return A list of service IDs that must be updated
     */
    Collection<String> bundleStopped(final Bundle aBundle);

    /**
     * Destroys the given proxy object
     * 
     * @param aProxy
     *            A proxy object
     */
    void destroyProxy(final Object aProxy);

    /**
     * Creates a proxy from the given remote service description, if possible
     * 
     * @param aRegistration
     *            A remote service registration bean
     * @param aFilteredInterfaces
     *            List of interfaces to give access to
     * @return A service proxy, null on error
     * @throws ClassNotFoundException
     *             The interface to proxify is not visible
     */
    Object getRemoteProxy(final RemoteServiceRegistration aRegistration,
            Collection<String> aFilteredInterfaces)
            throws ClassNotFoundException;
}