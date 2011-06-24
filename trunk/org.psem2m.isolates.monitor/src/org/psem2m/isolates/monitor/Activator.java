package org.psem2m.isolates.monitor;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.psem2m.utilities.files.CXFile;
import org.psem2m.utilities.files.CXFileDir;
import org.psem2m.utilities.logging.CActivityLoggerBasic;
import org.psem2m.utilities.logging.IActivityLogger;

public class Activator implements BundleActivator {

	/** Bundle ID */
	public static final String BUNDLE_ID = "isolates.monitor";

	/** Bundle context */
	private static BundleContext sBundleContext;

	/** Current bundle instance */
	private static Activator sCurrentInstance;

	/**
	 * Retrieves the current bundle context
	 * 
	 * @return The bundle context
	 */
	public static BundleContext getContext() {
		return sBundleContext;
	}

	/**
	 * Retrieves the activity logger
	 * 
	 * @return The activity logger
	 */
	public static IActivityLogger getLogger() {
		return sCurrentInstance.pLogger;
	}

	/** Logger */
	private IActivityLogger pLogger;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(final BundleContext aBundleContext) throws Exception {
		sBundleContext = aBundleContext;
		sCurrentInstance = this;

		// TODO call the configuration service
		// -Dorg.psem2m.platform.base=${workspace_loc}/psem2m/platforms/felix.user.dir/logs
		CXFileDir wBaseDir = new CXFileDir(
				System.getProperty("org.psem2m.platform.base"));
		CXFileDir wLogDir = new CXFileDir(wBaseDir, "var/log");

		if (!wLogDir.exists()) {
			wLogDir.createHierarchy();
		}
		CXFile wLogFile = new CXFile(wLogDir, BUNDLE_ID + "_%g.log");
		pLogger = CActivityLoggerBasic.newLogger(BUNDLE_ID,
				wLogFile.getAbsolutePath(), IActivityLogger.ALL,
				1024 * 1024 * 100, 5);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext aBundleContext) throws Exception {
		sBundleContext = null;
		sCurrentInstance = null;
		pLogger.close();
	}
}
