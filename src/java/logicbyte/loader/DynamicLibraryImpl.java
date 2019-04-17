package logicbyte.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.stream.LinkerObjectInputStream;
import logicbyte.bytecode.stream.LogicBytecodeInputStream;

/**
 *
 * @author Lochie
 */
public class DynamicLibraryImpl extends DynamicLibraryFile {

    static class DynamicLibraryLoadedObj {

	File linkerSrc;
	File bytecodeSrc;

	LinkerObjectFile linkerObj;
	LogicBytecodeFile bytecodeObj;

    }

    protected final File dir;

    protected Map<File, DynamicLibraryLoadedObj> loadedLinkers;

    public DynamicLibraryImpl(File dir) {
	this.dir = dir;
	this.loadedLinkers = new HashMap<>();
    }

    public void loadLinkers() throws IOException {

	this.loadedLinkers.clear();

	File[] files = this.dir.listFiles();

	FileInputStream fis = null;
	LinkerObjectInputStream linkerLoader = null;
	DynamicLibraryLoadedObj allocLoadedObj = null;

	try {
	    for (File f : files) {
		if (!f.isFile())
		    continue;
		if (!f.getName().endsWith(".o"))
		    continue;

		String name = f.getName();
		name = name.substring(0, name.length() - 2);
		File bytecodeFile = (new File(dir, name + ".lb"));

		if (!bytecodeFile.exists())
		    continue;

		allocLoadedObj = new DynamicLibraryLoadedObj();

		linkerLoader = new LinkerObjectInputStream(fis = new FileInputStream(f));
		allocLoadedObj.linkerSrc = f;
		allocLoadedObj.bytecodeSrc = bytecodeFile;
		allocLoadedObj.linkerObj = linkerLoader.readLinkerObject();
		fis.close();

		this.loadedLinkers.put(f, allocLoadedObj);
	    }
	} catch (IOException e) {
	    if (fis != null)
		try {
		    fis.close();
		} catch (IOException inner) {
		}
	    throw e;
	}
    }

    @Override
    public List<LinkerObjectFile> getLoadableList() {
	LinkedList<LinkerObjectFile> linkerObjs = new LinkedList<>();
	Iterator<DynamicLibraryLoadedObj> itx = this.loadedLinkers.values().iterator();
	while (itx.hasNext())
	    linkerObjs.add(itx.next().linkerObj);
	return linkerObjs;
    }

    @Override
    public LinkerObjectFile getBytecodeFor(File f) throws DynamicLibraryException {
	DynamicLibraryLoadedObj obj = this.loadedLinkers.get(f);
	return (obj != null) ? obj.linkerObj : null;
    }

    @Override
    public LogicBytecodeFile getBytecodeFor(LinkerObjectFile lo) throws DynamicLibraryException {
	Iterator<DynamicLibraryLoadedObj> itx = this.loadedLinkers.values().iterator();
	while (itx.hasNext()) {
	    DynamicLibraryLoadedObj obj = itx.next();
	    if (obj.linkerObj == lo) {
		if (obj.bytecodeObj != null)
		    return obj.bytecodeObj;
		synchronized ( obj ) {
		    File f = obj.bytecodeSrc;
		    if (!f.isFile())
			throw new DynamicLibraryException("bytecode file not a file");
		    FileInputStream fis = null;
		    try {
			LogicBytecodeInputStream bytecodeLoader = new LogicBytecodeInputStream(fis = new FileInputStream(f));
			obj.bytecodeObj = bytecodeLoader.readBytecodeFile();
			fis.close();
		    } catch (IOException ioex) {
			if (fis != null)
			    try {
				fis.close();
			    } catch (Exception ignore) {
				/* void */ }
			throw new DynamicLibraryException("I/O exception", ioex);
		    }
		}
		return obj.bytecodeObj;
	    }
	}
	return null;
    }

}
