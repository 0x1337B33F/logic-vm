package logicbyte.bytecode.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;

/**
 *
 * @author Lochie
 */
public class LinkerObjectOutputStream extends NeutralEncoderOutputStream {

    public LinkerObjectOutputStream(OutputStream o) {
	super(o);
    }

    public void writeLinkerObject(LinkerObjectFile obj) throws IOException {
	destDataStreamRef.write(new byte[] {0x13, (byte) 'L', (byte) 'S', (byte) 'o', (byte) 0xff});
	destDataStreamRef.writeShort((short) 1);
	destDataStreamRef.writeShort((short) 0); // technically unused ?

	if (obj.exportsTable != null && !obj.exportsTable.isEmpty())
	    destDataStreamRef.writeInt(obj.exportsTable.size());
	else
	    destDataStreamRef.writeInt(0);

	if (obj.importsTable != null && !obj.importsTable.isEmpty())
	    destDataStreamRef.writeInt(obj.importsTable.size());
	else
	    destDataStreamRef.writeInt(0);

	if (obj.typesTable != null && !obj.typesTable.isEmpty())
	    destDataStreamRef.writeInt(obj.typesTable.size());
	else
	    destDataStreamRef.writeInt(0);

	if (obj.globalsTable != null && !obj.globalsTable.isEmpty())
	    destDataStreamRef.writeInt(obj.globalsTable.size());
	else
	    destDataStreamRef.writeInt(0);

	Iterator<ExportsEntry> itx = obj.exportsTable.iterator();
	while (itx.hasNext())
	    writeExportsEntry(itx.next());

	Iterator<ImportsEntry> itx1 = obj.importsTable.iterator();
	while (itx1.hasNext())
	    writeImportsEntry(itx1.next());

	Iterator<TypesEntry> itx2 = obj.typesTable.iterator();
	while (itx2.hasNext())
	    writeTypesEntry(itx2.next());

	Iterator<GlobalsEntry> itx3 = obj.globalsTable.iterator();
	while (itx3.hasNext())
	    writeGlobalsEntry(itx3.next());

    }

    public void writeExportsEntry(ExportsEntry exports) throws IOException {
	destDataStreamRef.write((byte) exports.typeflags);
	writeString(exports.name);
	writePackedInt(exports.labels.size());
	Iterator<String> itx = exports.labels.iterator();
	while (itx.hasNext())
	    writeString(itx.next());
    }

    public void writeImportsEntry(ImportsEntry imports) throws IOException {
	destDataStreamRef.write((byte) imports.typeflags);
	writeString(imports.name);

	if ((imports.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD) == 0) {
	    writePackedInt(imports.labels.size());
	    Iterator<String> itx = imports.labels.iterator();
	    while (itx.hasNext())
		writeString(itx.next());

	    if ((imports.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
		writePackedInt(imports.slots.size());

		Iterator<Integer> itx1 = imports.slots.iterator();
		while (itx1.hasNext())
		    writePackedInt(itx1.next());
	    }
	}
    }

    public void writeTypesEntry(TypesEntry types) throws IOException {
	writePackedInt(types.slot);
	destDataStreamRef.write((byte) types.typeflags);
	writeString(types.name);
	if ((types.typeflags & LinkerObjectFile.TYPEFLAGS_TYPES_USER) != 0)
	    writeString(types.superName);
    }

    public void writeGlobalsEntry(GlobalsEntry globals) throws IOException {
	writePackedInt(globals.type);
	destDataStreamRef.write((byte) globals.globalsflags);
	writeString(globals.name);
    }

}
