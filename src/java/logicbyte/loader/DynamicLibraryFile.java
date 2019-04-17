package logicbyte.loader;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;

public abstract class DynamicLibraryFile {

    public static class DynamicLibraryException extends Exception {

	public DynamicLibraryException(String message) {
	    super(message);
	}

	public DynamicLibraryException(String message, Throwable inner) {
	    super(message, inner);
	}
    }

    public abstract List<LinkerObjectFile> getLoadableList();

    public abstract LogicBytecodeFile getBytecodeFor(LinkerObjectFile lo) throws DynamicLibraryException;

    public abstract LinkerObjectFile getBytecodeFor(File f) throws DynamicLibraryException;

    public LinkerObjectFile getBytecodeFor(String libraryName) {
	List<LinkerObjectFile> ptr = getLoadableList();
	if (ptr == null)
	    return null;
	Iterator<LinkerObjectFile> itx = ptr.iterator();
	while (itx.hasNext()) {
	    LinkerObjectFile linkInfo = itx.next();
	    if (linkInfo.exports(libraryName))
		return linkInfo;
	}
	return null;
    }

}
