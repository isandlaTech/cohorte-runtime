package org.psem2m.isolates.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.base.activators.CActivatorBase;

public class CIsolatesUiActivator extends CActivatorBase implements
        BundleActivator {

    /** Current instance **/
    private static CIsolatesUiActivator sSingleton = null;

    /**
     * Retrieves the current bundle instance
     * 
     * @return The bundle instance
     */
    public static CIsolatesUiActivator getInstance() {

        return sSingleton;
    }

    /**
     * @return
     */
    @Override
    public IIsolateLoggerSvc getIsolateLoggerSvc() {

        return super.getIsolateLoggerSvc();
    }

    /**
     * @return
     */
    @Override
    public boolean hasIsolateLoggerSvc() {

        return getIsolateLoggerSvc() != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        // Store the singleton reference
        sSingleton = this;

        super.start(bundleContext);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext bundleContext) throws Exception {

        super.stop(bundleContext);

        // Forget the singleton reference
        sSingleton = null;
    }
}
