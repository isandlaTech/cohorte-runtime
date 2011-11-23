/*******************************************************************************
 * Copyright (c) 2011 www.isandlatech.com (www.isandlatech.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ogattaz  (isandlaTech) - 22 nov. 2011 - initial API and implementation
 *******************************************************************************/
package org.psem2m.composer.ui;

import java.util.List;
import java.util.concurrent.Executor;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleException;
import org.psem2m.composer.CompositionSnapshot;
import org.psem2m.composer.IComposer;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.base.activators.CPojoBase;
import org.psem2m.isolates.ui.admin.api.EUiAdminFont;
import org.psem2m.isolates.ui.admin.api.EUiAdminPanelLocation;
import org.psem2m.isolates.ui.admin.api.IUiAdminPanel;
import org.psem2m.isolates.ui.admin.api.IUiAdminPanelControler;
import org.psem2m.isolates.ui.admin.api.IUiAdminSvc;

/**
 * @author ogattaz
 * 
 */

@Component(name = "psem2m-composer-ui-admin-factory", publicFactory = false)
@Instantiate(name = "psem2m-composer-ui-admin")
public class CUiAdminPanelComposition extends CPojoBase implements
        IUiAdminPanelControler {

    @Requires
    IComposer pComposer;

    private List<CompositionSnapshot> pCompositionSnapshots = null;

    /** */
    private CCompositionTreeModel pCompositionTreeModel = null;

    /** the JPanel **/
    private CJPanelComposition pJPanel = null;

    /** The logger */
    @Requires
    private IIsolateLoggerSvc pLogger;

    /** the UiAdminPanel returned by the IUiAdminScv */
    private IUiAdminPanel pUiAdminPanel = null;

    /** the IUiAdminScv */
    @Requires
    IUiAdminSvc pUiAdminSvc;

    /**
     * Service reference managed by iPojo (see metadata.xml)
     */
    @Requires(filter = "(thread=main)")
    private Executor pUiExecutor;

    /**
     * 
     */
    public CUiAdminPanelComposition() {

        super();
        //
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#invalidatePojo()
     */
    @Override
    @Invalidate
    public void invalidatePojo() throws BundleException {

        // logs in the bundle output
        pLogger.logInfo(this, "invalidatePojo", "INVALIDATE", toDescription());

        try {
            pCompositionSnapshots = null;

            if (pCompositionTreeModel != null) {
                pCompositionTreeModel.destroy();
                pCompositionTreeModel = null;
            }
            if (pJPanel != null) {

                pJPanel.destroy();
                pJPanel = null;
            }
            pUiAdminSvc.removeUiAdminPanel(pUiAdminPanel);

        } catch (Exception e) {
            pLogger.logSevere(this, "invalidatePojo", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.api.IUiAdminPanelControler#setUiAdminFont
     * (org.psem2m.isolates.ui.admin.api.EUiAdminFont)
     */
    @Override
    public void setUiAdminFont(final EUiAdminFont aUiAdminFont) {

        pJPanel.setTextFont(aUiAdminFont);
        pJPanel.setTreeFont(aUiAdminFont);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#validatePojo()
     */
    @Override
    @Validate
    public void validatePojo() throws BundleException {

        // logs in the bundle output
        pLogger.logInfo(this, "invalidatePojo", "VALIDATE", toDescription());

        try {
            pUiAdminPanel = pUiAdminSvc.newUiAdminPanel("Composition",
                    "Bundles list and managment.", null, this,
                    EUiAdminPanelLocation.FIRST);

            pCompositionSnapshots = pComposer.getCompositionSnapshot();

            CompositionSnapshot wCompositionSnapshot = pCompositionSnapshots
                    .size() > 0 ? pCompositionSnapshots.get(0) : null;

            pCompositionTreeModel = new CCompositionTreeModel(
                    wCompositionSnapshot);

            pJPanel = new CJPanelComposition(pUiExecutor, pLogger,
                    pUiAdminPanel.getPanel(), pCompositionTreeModel);

        } catch (Exception e) {
            pLogger.logSevere(this, "validatePojo", e);
        }
    }
}