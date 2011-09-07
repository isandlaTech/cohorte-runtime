/**
 * File:   CFileFinderSvc.java
 * Author: Thomas Calmant
 * Date:   6 sept. 2011
 */
package org.psem2m.isolates.base.dirs.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.psem2m.isolates.base.dirs.IFileFinderSvc;
import org.psem2m.isolates.base.dirs.IPlatformDirsSvc;

/**
 * Simple file finder : tries to find the given file in the platform main
 * directories.
 * 
 * @author Thomas Calmant
 */
public class CFileFinderSvc implements IFileFinderSvc {

    /** Platform directories service */
    private IPlatformDirsSvc pPlatformDirs;

    /**
     * Default constructor (for iPOJO)
     */
    public CFileFinderSvc() {
	super();
    }

    /**
     * Constructor without injection
     * 
     * @param aPlatformDirs
     *            Platform directory service instance
     */
    public CFileFinderSvc(final IPlatformDirsSvc aPlatformDirs) {
	super();
	pPlatformDirs = aPlatformDirs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.dirs.IFileFinderSvc#find(java.io.File,
     * java.lang.String)
     */
    @Override
    public File[] find(final File aBaseFile, final String aFileName) {

	// Use a set to avoid duplicates
	final Set<File> foundFiles = new LinkedHashSet<File>();

	if (aBaseFile != null) {
	    // Try to be relative to the parent, if the base file is a file
	    File baseDir = null;

	    if (aBaseFile.isFile()) {
		// Base file is a file : get its parent directory
		baseDir = aBaseFile.getParentFile();

	    } else if (aBaseFile.isDirectory()) {
		// Use the directory
		baseDir = aBaseFile;
	    }

	    if (baseDir != null) {
		// We have a valid base
		final File testRelFile = new File(baseDir, aFileName);
		if (testRelFile.exists()) {
		    // Found !
		    foundFiles.add(testRelFile);
		}
	    }
	}

	// In any case, try using only the file name
	foundFiles.addAll(internalFind(aFileName));
	return foundFiles.toArray(new File[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.dirs.IFileFinderSvc#find(java.lang.String)
     */
    @Override
    public File[] find(final String aFileName) {

	final List<File> foundFiles = internalFind(aFileName);
	if (foundFiles.isEmpty()) {
	    // Return null if no file was found
	    return null;
	}

	return foundFiles.toArray(new File[0]);
    }

    /**
     * Tries to find the given file in the platform directories. Never returns
     * null.
     * 
     * @param aFileName
     *            Name of the file to search for
     * @return The list of the corresponding files (never null, can be empty)
     */
    protected List<File> internalFind(final String aFileName) {

	final List<File> foundFiles = new ArrayList<File>();

	// Test on each PSEM2M root directory
	for (File rootDir : pPlatformDirs.getPlatformRootDirs()) {

	    final File testFile = new File(rootDir, aFileName);
	    if (testFile.exists()) {
		foundFiles.add(testFile);
	    }
	}

	return foundFiles;
    }
}
