package logicbyte.bytecode.validate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;

/**
 *
 * @author Lochie
 */
public class LogicBytecodeValidator {

    /**
     * The lookup map of type-names to type import declarations
     */
    private final Map<Integer, ImportsEntry> mapTypesImports = new HashMap<>();

    /**
     * The lookup map of type-names to type declarations
     */
    private final Map<Integer, TypesEntry> mapTypesDecls = new HashMap<>();

    /**
     * The lookup map of global-names to global import declarations
     */
    private final Map<String, ImportsEntry> mapGlobalsImports = new HashMap<>();
    /**
     * The lookup map of global-names to global declarations
     */
    private final Map<String, GlobalsEntry> mapGlobalsDecls = new HashMap<>();

    /**
     * The lookup map of function-names to function import declarations
     */
    private final Map<String, ImportsEntry> mapFunctionImports = new HashMap<>();
    /**
     * The lookup map of function-names to function declarations
     */
    private final Map<String, BytecodeFunctionDecl> mapFunctionDecls = new HashMap<>();

    /**
     * The Stack used to validate stack usage in functions
     */
    private final Stack<Integer> stackStackTypes = new Stack<>();

    private void reset() {
	stackStackTypes.clear();
    }

    public void validate(LinkerObjectFile linker, LogicBytecodeFile bytecode) throws LinkerValidationException, BytecodeValidationException {
	reset();
	generateTypesTables(linker, bytecode);
	generateFunctionTables(linker, bytecode);

    }

    private void generateTypesTables(LinkerObjectFile linker, LogicBytecodeFile bytecode) {
	if (linker.importsTable != null && !linker.importsTable.isEmpty()) {
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry entry = itx.next();
		if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
		    Iterator<Integer> itx1 = entry.slots.iterator();
		    while (itx1.hasNext())
			this.mapTypesImports.put(itx1.next(), entry);
		}
	    }
	}

	if (linker.typesTable != null && !linker.typesTable.isEmpty()) {
	    Iterator<TypesEntry> itx = linker.typesTable.iterator();
	    while (itx.hasNext()) {
		TypesEntry entry = itx.next();
		this.mapTypesDecls.put(entry.slot, entry);
	    }
	}
    }

    private void generateFunctionTables(LinkerObjectFile linker, LogicBytecodeFile bytecode) {

	if (linker.importsTable != null && !linker.importsTable.isEmpty()) {
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		/* FIXME: wildcard imports require lookup usage; this requires we've already validated
		 the target linker / bytecode. */
	    }
	}

    }

}
