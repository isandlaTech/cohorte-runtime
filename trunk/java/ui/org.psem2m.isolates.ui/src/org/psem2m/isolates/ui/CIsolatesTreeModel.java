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
package org.psem2m.isolates.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.psem2m.isolates.services.monitoring.IIsolatePresenceListener;
import org.psem2m.signals.ISignalDirectory;

/**
 * @author ogattaz
 * 
 */
public class CIsolatesTreeModel implements TreeModel, IIsolatePresenceListener {

    private final ISignalDirectory pSignalDirectory;

    /** The snapshots list */
    private List<CSnapshotNode> pSnapshotNodes = new ArrayList<CSnapshotNode>();

    /**
     * Sets up the tree model
     * 
     * @param aCompositionSnapshot
     *            A composition snapshot list
     */
    CIsolatesTreeModel(final ISignalDirectory aSignalDirectory) {

        super();
        pSignalDirectory = aSignalDirectory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.
     * TreeModelListener)
     */
    @Override
    public void addTreeModelListener(final TreeModelListener aListener) {

    }

    /**
     * Cleans up the model
     */
    void destroy() {

        pSnapshotNodes.clear();
        pSnapshotNodes = null;
    }

    /**
     * @param aId
     * @return
     */
    private CSnapshotNode findNode(final String aId) {

        synchronized (pSnapshotNodes) {

            final int index = findNodeIdx(aId);
            if (index < 0) {
                return null;
            }

            return pSnapshotNodes.get(index);
        }
    }

    /**
     * @param aId
     * @return
     */
    private int findNodeIdx(final String aId) {

        synchronized (pSnapshotNodes) {
            int wIdx = 0;
            for (final CSnapshotNode wNode : pSnapshotNodes) {
                if (wNode.getName().equals(aId)) {
                    return wIdx;
                }
                wIdx++;
            }
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    @Override
    public Object getChild(final Object aParent, final int aIndex) {

        if (aParent instanceof String) {
            // Root
            synchronized (pSnapshotNodes) {
                return pSnapshotNodes.get(aIndex);
            }
        }

        final CSnapshotAbstract wCompositionSnapshot = (CSnapshotAbstract) aParent;
        return wCompositionSnapshot.getChild(aIndex);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    @Override
    public int getChildCount(final Object aParent) {

        if (aParent instanceof String) {
            // Root
            synchronized (pSnapshotNodes) {
                return pSnapshotNodes.size();
            }
        }

        final CSnapshotAbstract wCompositionSnapshot = (CSnapshotAbstract) aParent;
        return wCompositionSnapshot.getChildCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public int getIndexOfChild(final Object aParent, final Object aChild) {

        if (aParent instanceof String) {
            // Root
            synchronized (pSnapshotNodes) {
                return pSnapshotNodes.indexOf(aChild);
            }
        }

        final CSnapshotAbstract wCompositionSnapshot = (CSnapshotAbstract) aParent;
        return wCompositionSnapshot.getIndexOfChild((CSnapshotAbstract) aChild);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getRoot()
     */
    @Override
    public Object getRoot() {

        return "Isolates";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.services.monitoring.IIsolatePresenceListener#
     * handleIsolatePresence(java.lang.String, java.lang.String,
     * org.psem2m.isolates
     * .services.monitoring.IIsolatePresenceListener.EPresence)
     */
    @Override
    public void handleIsolatePresence(final String aIsolateId,
            final String aNodeId, final EPresence aPresence) {

        CSnapshotNode wNode = findNode(aNodeId);

        switch (aPresence) {
        case REGISTERED: {
            synchronized (pSnapshotNodes) {
                // Find or create the node
                if (wNode == null) {
                    wNode = new CSnapshotNode(aNodeId);
                    wNode.setHostName(pSignalDirectory.getHostForNode(aNodeId));

                    // Store the new node
                    pSnapshotNodes.add(wNode);
                }

                // Store the isolate
                final CSnapshotIsolate snapshot = new CSnapshotIsolate(
                        aIsolateId);
                snapshot.setHostAccess(pSignalDirectory
                        .getIsolateAccess(aIsolateId));
                wNode.add(snapshot);
            }
            break;
        }

        case UNREGISTERED: {
            // Find the node index in the list
            synchronized (pSnapshotNodes) {
                if (wNode != null) {
                    wNode.removeChild(aIsolateId);
                    if (wNode.getChildCount() <= 0) {
                        pSnapshotNodes.remove(wNode);
                    }
                }
            }
            break;
        }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
     */
    @Override
    public boolean isLeaf(final Object aObject) {

        if (aObject instanceof String) {
            synchronized (pSnapshotNodes) {
                return pSnapshotNodes.isEmpty();
            }
        }

        return aObject instanceof CSnapshotIsolate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.
     * TreeModelListener)
     */
    @Override
    public void removeTreeModelListener(final TreeModelListener aListener) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath,
     * java.lang.Object)
     */
    @Override
    public void valueForPathChanged(final TreePath aArg0, final Object aArg1) {

    }
}