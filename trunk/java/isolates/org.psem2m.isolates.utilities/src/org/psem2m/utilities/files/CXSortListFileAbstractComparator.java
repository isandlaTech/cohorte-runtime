package org.psem2m.utilities.files;

import java.io.File;

import org.psem2m.utilities.CXAbstractListComparator;

 /**
 * Renvoie un comparateur pour classer les fichiers par date de modification
 * 
 * @author Sage Grenoble
 * 
 * @param <E>
 */
public abstract class CXSortListFileAbstractComparator<E> extends CXAbstractListComparator<E>
{

	/**
	 * 
	 */
	public CXSortListFileAbstractComparator()
	{
		super();
	}

	/**
	 * @param aSortAsc
	 */
	public CXSortListFileAbstractComparator(boolean aSortAsc)
	{
		super(aSortAsc);
	}

	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	abstract protected int compareFiles(File a, File b);

	/* (non-Javadoc)
	 * @see org.psem2m.utilities.CXAbstractListComparator#compareObjects(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected int compareObjects(Object a, Object b)
	{
		return compareFiles((File) a, (File) b);
	}

	/* (non-Javadoc)
	 * @see org.psem2m.utilities.CXAbstractListComparator#equalsObjects(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected boolean equalsObjects(Object a, Object b)
	{
		// A voir si on peut utilise Files.equals()
		String wStA = ((File) a).getPath();
		String wStB = ((File) b).getPath();
		return wStA.equals(wStB);
	}
}

/**
 * Renvoie un comparateur pour classer les fichiers par date de modification
 * 
 * @author parents
 * 
 */
class CAdminFileDateComparator extends CXSortListFileAbstractComparator<File>
{

	/**
	 * 
	 */
	public CAdminFileDateComparator()
	{
		super();
	}

	/**
	 * @param aSortAsc
	 */
	public CAdminFileDateComparator(boolean aSortAsc)
	{
		super(aSortAsc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.CAbstractFileComparator#compareFiles(java.io.File,
	 * java.io.File)
	 */
	@Override
	protected int compareFiles(File a, File b)
	{
		Long wA = new Long(a.lastModified());
		Long wB = new Long(b.lastModified());
		return wA.compareTo(wB);
	}
}

/**
 * Renvoie un comparateur pour classer les fichiers par taille
 * 
 * @author parents
 * 
 */
class CAdminFilePathComparator extends CXSortListFileAbstractComparator<File>
{

	/**
	 * 
	 */
	public CAdminFilePathComparator()
	{
		super();
	}

	/**
	 * @param aSortAsc
	 */
	public CAdminFilePathComparator(boolean aSortAsc)
	{
		super(aSortAsc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.CAbstractFileComparator#compareFiles(java.io.File,
	 * java.io.File)
	 */
	@Override
	protected int compareFiles(File a, File b)
	{
		// suppression de la disticntion majucule minuscule
		// sous MacOsX et Windows, les fichiers "aaa" et "AAA" sont identiques
		String wA = a.getPath().toLowerCase();
		String wB = b.getPath().toLowerCase();;
		return  wA.compareTo(wB);
	}
}

/**
 * Renvoie un comparateur pour classer les fichiers par taille
 * 
 * @author parents
 * 
 */
class CAdminFileSizeComparator extends CXSortListFileAbstractComparator<File>
{

	/**
	 * 
	 */
	public CAdminFileSizeComparator()
	{
		super();
	}

	/**
	 * @param aSortAsc
	 */
	public CAdminFileSizeComparator(boolean aSortAsc)
	{
		super(aSortAsc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.CAbstractFileComparator#compareFiles(java.io.File,
	 * java.io.File)
	 */
	@Override
	protected int compareFiles(File a, File b)
	{
		Long wA = new Long(a.length());
		Long wB = new Long(b.length());
		return wA.compareTo(wB);
	}
}