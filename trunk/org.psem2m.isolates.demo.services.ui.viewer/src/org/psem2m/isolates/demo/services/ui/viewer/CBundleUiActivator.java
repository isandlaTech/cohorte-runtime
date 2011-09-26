package org.psem2m.isolates.demo.services.ui.viewer;

import org.osgi.framework.BundleContext;
import org.psem2m.isolates.base.activators.CActivatorBase;
import org.psem2m.isolates.base.activators.IActivatorBase;

public class CBundleUiActivator extends CActivatorBase implements
        IActivatorBase {

    /** first instance **/
    private static CBundleUiActivator sSingleton = null;

    /**
     * @return
     */
    public static CBundleUiActivator getInstance() {

        return sSingleton;
    }

    /**
     * Explicit default constructor
     */
    public CBundleUiActivator() {

        super();
        if (sSingleton == null) {
            sSingleton = this;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.CXObjectBase#destroy()
     */
    @Override
    public void destroy() {

        // nothing...
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

        super.start(bundleContext);
        // ...
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
        // ...
    }

}
