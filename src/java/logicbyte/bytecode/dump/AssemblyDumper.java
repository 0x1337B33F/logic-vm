package logicbyte.bytecode.dump;

import java.io.PrintWriter;
import java.util.Iterator;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.Opcode;

/**
 * <p>
 * Displays loaded object(s) to a desired PrintWriter to quickly check the linker or bytecode data written to a struct instance. Not a
 * disassembler, just a byte-to-object display thing.</p>
 *
 * @author Lochie
 */
public class AssemblyDumper {

    public static void dump(LinkerObjectFile linker, PrintWriter output) {
        output.print("LinkerObject{");
        output.print(String.format("Name: `%s`, ", linker.source()));
        output.print(String.format("Exports: %s, ", linker.exportsTable.size()));
        output.print(String.format("Imports: %s, ", linker.importsTable.size()));
        output.print(String.format("Types: %s, ", linker.typesTable.size()));
        output.print(String.format("Globals: %s", linker.globalsTable.size()));
        output.println("}:");

        Iterator<ExportsEntry> itx0 = linker.exportsTable.iterator();
        while (itx0.hasNext()) {
            dumpExports(itx0.next(), output);
        }
        Iterator<ImportsEntry> itx1 = linker.importsTable.iterator();
        while (itx1.hasNext()) {
            dumpImports(itx1.next(), output);
        }
        Iterator<TypesEntry> itx2 = linker.typesTable.iterator();
        while (itx2.hasNext()) {
            dumpTypes(itx2.next(), output);
        }
        Iterator<GlobalsEntry> itx3 = linker.globalsTable.iterator();
        while (itx3.hasNext()) {
            dumpGlobals(itx3.next(), output);
        }
    }

    public static void dump(LogicBytecodeFile bytecode, PrintWriter output) {
        output.print("LogicBytecode{");
        output.print(String.format("Name: `%s`, ", bytecode.source()));
        output.println("}:");
        BytecodeFunctionDecl[] funcs = bytecode.getAll();
        for (BytecodeFunctionDecl decl : funcs) {
            dumpFunctionDecl(decl, output);
        }
    }

    public static void dumpExports(ExportsEntry entry, PrintWriter output) {
        output.print(String.format("\tExport: %s, ", entry.name));

        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE) != 0) {
            output.print("TYPE export, ");
        }
        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_GLOBAL) != 0) {
            output.print("GLOBAL export, ");
        }
        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_FUNCTION) != 0) {
            output.print("FUNCTION export, ");
        }

        output.println(String.format("%s symbols:", entry.labels.size()));

        Iterator<String> itx = entry.labels.iterator();
        while (itx.hasNext()) {
            output.println(String.format("\t\tsymbol `%s`", itx.next()));
        }
    }

    public static void dumpImports(ImportsEntry entry, PrintWriter output) {
        output.print(String.format("\tImport: %s, ", entry.name));

        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
            output.print("TYPE import, ");
        }
        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL) != 0) {
            output.print("GLOBAL import, ");
        }
        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_FUNCTION) != 0) {
            output.print("FUNCTION import, ");
        }

        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD) != 0) {
            output.println("WILDCARD import.");
        } else {
            output.println(String.format("%s symbols:", entry.labels.size()));

            Iterator<String> itx = entry.labels.iterator();

            if (entry.slots != null) {
                Iterator<Integer> itx1 = entry.slots.iterator();
                while (itx.hasNext()) {
                    output.println(String.format("\t\tsymbol `%s`, slot %s", itx.next(), itx1.next()));
                }
            } else {
                while (itx.hasNext()) {
                    output.println(String.format("\t\tsymbol `%s`", itx.next()));
                }
            }
        }

    }

    public static void dumpTypes(TypesEntry entry, PrintWriter output) {
        output.print(String.format("\tType: %s, slot %s, ", entry.name, entry.slot));
        if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_TYPES_USER) != 0) {
            output.println(String.format("supertype of user-type `%s`", entry.superName));
        } else if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_TYPES_HANDLE) != 0) {
            output.println("supertype of handle type");
        } else {
            output.println("(invalid)");
        }
    }

    public static void dumpGlobals(GlobalsEntry entry, PrintWriter output) {
        output.print(String.format("\tGlobal: %s, type %s, ", entry.name, entry.type));
        if ((entry.globalsflags & LinkerObjectFile.TYPEFLAGS_GLOBAL_ISNATIVE) != 0) {
            output.println("native global value");
        } else {
            output.println("user global value");
        }
    }

    public static void dumpFunctionDecl(BytecodeFunctionDecl decl, PrintWriter output) {
        output.print(String.format("\tFunction `%s`, ", decl.name()));

        if (decl.isnfunc()) {
            output.print("native func, ");
        } else {
            output.print("user func, ");
        }

        if (decl.hasargs()) {
            output.print(String.format("has %s args, ", decl.numargs()));
        } else {
            output.print("no args, ");
        }

        if (decl.hasreturn()) {
            output.print(String.format("returns type `%s`, ", decl.returntype()));
        } else {
            output.print(String.format("returns nothing, "));
        }

        if (decl.isufunc()) {
            if (decl.haslocals()) {
                output.print(String.format("has %s locals, ", decl.numlocals()));
            } else {
                output.print("no locals, ");
            }
        }

        if (decl.hasdebug()) {
            output.println("has debug info.");
        } else {
            output.println("no debug info.");
        }

        if (decl.hasargs()) {
            for (int i = 0, j = decl.numargs(); i < j; i++) {
                output.println(String.format("\t\targ %s: type %s", i, decl.argtype(i)));
            }
        }

        if (decl.isufunc() && decl.haslocals()) {
            for (int i = 0, j = decl.numlocals(); i < j; i++) {
                if (decl.localarray(i)) {
                    output.println(String.format("\t\tlocal %s: array type %s", i, decl.localtype(i)));
                } else {
                    output.println(String.format("\t\tlocal %s: type %s", i, decl.localtype(i)));
                }
            }
        }

        if (decl.isufunc()) {
            FunctionOpcodeList ops = decl.opcodes();
            output.println("\t\tOpcodes:");
            for (int i = 0, j = ops.size(); i < j; i++) {
                Opcode opcode = ops.get(i), memonic = opcode.opcode();
                output.print(String.format("\t\t\t%s: %s ", i, memonic.toString()));
                if (opcode.hasopts()) {
                    output.print(String.format("(%s) ", opcode.getopts()));
                }

                if (opcode.args() != 0) {
                    for (int k = 0, l = opcode.args(); k < l; k++) {
                        output.print(String.format("\t%s", opcode.argobject(k)));
                    }
                }
                output.println(";");
            }
        }

        if (decl.hasdebug()) {
            dumpFunctionDeclDebug(decl.debug(), output);
        }
    }

    public static void dumpFunctionDeclDebug(DebugSegment debug, PrintWriter output) {
        output.print("\t\tDebugInfo{");
        output.print((debug.hasargnames()) ? "has argnames, " : "no argnames, ");
        output.print((debug.haslocalnames()) ? "has localnames, " : "no localnames, ");
        output.print((debug.haslocations()) ? "has locations" : "no locations");
        output.println("}:");

        if (debug.hasargnames()) {
            output.println("\t\t\targuments:");
            for (int i = 0, j = debug.numargnames(); i < j; i++) {
                output.println(String.format("\t\t\t\t%s: %s", i, debug.argname(i)));
            }
        }

        if (debug.haslocalnames()) {
            output.println("\t\t\tlocals:");
            for (int i = 0, j = debug.numlocalnames(); i < j; i++) {
                output.println(String.format("\t\t\t\t%s: %s", i, debug.localname(i)));
            }
        }

        if (debug.haslocations()) {
            output.println("\t\t\tlocations:");
            for (int i = 0, j = debug.numlocations(); i < j; i++) {
                output.print(String.format("\t\t\t\t%s: addr %s, ", i, debug.locaddr(i)));
                DebugLocationSegment seg = debug.loctag(i);
                output.println(String.format("line %s, col %s", seg.line, seg.col));
            }
        }
    }
}
