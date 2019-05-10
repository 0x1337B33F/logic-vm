package logicbyte.bytecode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LinkerObjectFile {

    /**
     * Exports is a TYPE export
     */
    public static final byte TYPEFLAGS_EXPORTS_TYPE = 0b00000001;
    /**
     * Exports is a GLOBALS export
     */
    public static final byte TYPEFLAGS_EXPORTS_GLOBAL = 0b00000010;
    /**
     * Exports is a FUNCTION export
     */
    public static final byte TYPEFLAGS_EXPORTS_FUNCTION = 0b00000100;

    /**
     * EXPORTSENTRY: entry indicating a set of tokens of a type which are implemented within the bytecode associated with the linker file.
     */
    public static class ExportsEntry {

	/**
	 * The type of EXPORTS; bitmask.
	 */
	public byte typeflags;
	/**
	 * The name of the EXPORTS.
	 */
	public String name;
	/**
	 * The list of symbol(s) of the type.
	 */
	public List<String> labels;
    }

    /**
     * Imports is a TYPE export
     */
    public static final byte TYPEFLAGS_IMPORTS_TYPE = 0b00000001;
    /**
     * Imports is a GLOBALS export
     */
    public static final byte TYPEFLAGS_IMPORTS_GLOBAL = 0b00000010;
    /**
     * Imports is a FUNCTION export
     */
    public static final byte TYPEFLAGS_IMPORTS_FUNCTION = 0b00000100;

    /**
     * Imports is a WILDCARD FUNCTION import
     */
    public static final byte TYPEFLAGS_IMPORTS_WILDCARD = 0b01000000;

    /**
     * IMPORTSENTRY: entry indicating a set of tokens of a type which must be imported for successful execution of the bytecode associated
     * with the linker file.
     */
    public static class ImportsEntry {

	/**
	 * The type of IMPORTS; bitmask.
	 */
	public byte typeflags;
	/**
	 * The name of the IMPORTS.
	 */
	public String name;
	/**
	 * The list of symbol(s) of the type.
	 */
	public List<String> labels;
	/**
	 * The slot assignment, used when the type of IMPORTS is the TYPE type; the value of the integer in each index indicates the
	 * assigned Type_Number for the type when imported for expansion of the referent Type_Number within the bytecode.
	 */
	public List<Integer> slots;
    }

    public static final byte TYPEFLAGS_TYPES_HANDLE = 0b00000001;
    public static final byte TYPEFLAGS_TYPES_USER = 0b00000010;

    /**
     * TYPESENTRY: entry indicating the declaration of a type.
     */
    public static class TypesEntry {

	public int slot;
	public byte typeflags;
	public String name;
	public String superName;
    }

    public static final byte TYPEFLAGS_GLOBAL_ISNATIVE = 0b00000001;
    public static final byte TYPEFLAGS_GLOBAL_ISARRAY = 0b00000010;

    /**
     * GLOBALSENTRY: entry indicating the declaration of a global.
     */
    public static class GlobalsEntry {

	public int type;
	public byte globalsflags;
	public String name;

    }

    final String sourceName;

    public final List<ExportsEntry> exportsTable = new LinkedList<>();

    public final List<ImportsEntry> importsTable = new LinkedList<>();

    public final List<TypesEntry> typesTable = new LinkedList<>();

    public final List<GlobalsEntry> globalsTable = new LinkedList<>();

    public LinkerObjectFile(String sourceName) {
	this.sourceName = sourceName;
    }

    public String source() {
	return this.sourceName;
    }

    public final boolean exports(String name) {
	if (exportsTable != null && !exportsTable.isEmpty()) {
	    Iterator<ExportsEntry> itx1 = exportsTable.iterator();
	    while (itx1.hasNext()) {
		ExportsEntry exportSymbolSet = itx1.next();
		if (exportSymbolSet.name.equals(name))
		    return true;
	    }
	}
	return false;
    }

    public final boolean imports(String name) {
	if (importsTable != null && !importsTable.isEmpty()) {
	    Iterator<ImportsEntry> itx1 = importsTable.iterator();
	    while (itx1.hasNext()) {
		ImportsEntry importSymbolSet = itx1.next();
		if (importSymbolSet.name.equals(name))
		    return true;
	    }
	}
	return false;
    }

    public final boolean type(String name) {
	if (typesTable != null && !typesTable.isEmpty()) {
	    Iterator<TypesEntry> itx1 = typesTable.iterator();
	    while (itx1.hasNext()) {
		TypesEntry typeEntry = itx1.next();
		if (typeEntry.name.equals(name))
		    return true;
	    }
	}
	return false;
    }

    public final boolean global(String name) {
	if (globalsTable != null && !globalsTable.isEmpty()) {
	    Iterator<GlobalsEntry> itx1 = globalsTable.iterator();
	    while (itx1.hasNext()) {
		GlobalsEntry globalEntry = itx1.next();
		if (globalEntry.name.equals(name))
		    return true;
	    }
	}
	return false;
    }

    public final List<ExportsEntry> getexport(String name) {
	if (exportsTable != null && !exportsTable.isEmpty()) {
	    LinkedList<ExportsEntry> result = new LinkedList<>();
	    Iterator<ExportsEntry> itx1 = exportsTable.iterator();
	    while (itx1.hasNext()) {
		ExportsEntry exportSymbolSet = itx1.next();
		if (exportSymbolSet.name.equals(name))
		    result.add(exportSymbolSet);
	    }
	    return (!result.isEmpty()) ? result : null;
	}
	return null;
    }

    public final List<ImportsEntry> getimport(String name) {
	if (importsTable != null && !importsTable.isEmpty()) {
	    LinkedList<ImportsEntry> result = new LinkedList<>();
	    Iterator<ImportsEntry> itx1 = importsTable.iterator();
	    while (itx1.hasNext()) {
		ImportsEntry importSymbolSet = itx1.next();
		if (importSymbolSet.name.equals(name))
		    result.add(importSymbolSet);
	    }
	    return (!result.isEmpty()) ? result : null;
	}
	return null;
    }

    public final TypesEntry gettype(String name) {
	if (typesTable != null && !typesTable.isEmpty()) {
	    Iterator<TypesEntry> itx1 = typesTable.iterator();
	    while (itx1.hasNext()) {
		TypesEntry typeEntry = itx1.next();
		if (typeEntry.name.equals(name))
		    return typeEntry;
	    }
	}
	return null;
    }

    public final GlobalsEntry getglobal(String name) {
	if (globalsTable != null && !globalsTable.isEmpty()) {
	    Iterator<GlobalsEntry> itx1 = globalsTable.iterator();
	    while (itx1.hasNext()) {
		GlobalsEntry globalEntry = itx1.next();
		if (globalEntry.name.equals(name))
		    return globalEntry;
	    }
	}
	return null;
    }

}
