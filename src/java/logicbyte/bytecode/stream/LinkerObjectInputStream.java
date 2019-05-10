package logicbyte.bytecode.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;

/**
 *
 * @author Lochie
 */
public class LinkerObjectInputStream extends NeutralDecoderInputStream {
    
    final String sourceName;

    public LinkerObjectInputStream(InputStream i, String sourceName) {
	super(i);
	this.sourceName = sourceName;
    }

    public LinkerObjectFile readLinkerObject() throws IOException {
	byte[] header = new byte[ 5 ];
	if (srcDataStreamRef.read(header) != header.length)
	    throw new IOException("Not enough header bytes");

	if (header[0] != (byte) 0x13 || header[1] != (byte) 'L' || header[2] != (byte) 'S' || header[3] != (byte) 'o' || header[4] != (byte) 0xFF)
	    throw new IOException(String.format("Invalid header bytes: %2x %2x %2x %2x %2x", header[0], header[1], header[2], header[3],
		    header[4]));

	short version = srcDataStreamRef.readShort(),
		flags = srcDataStreamRef.readShort();

	if (version != (short) 1 || flags != (short) 0)
	    throw new IOException("Invalid version or flags");

	int exportTableSz = srcDataStreamRef.readInt(),
		importTableSz = srcDataStreamRef.readInt(),
		typesTableSz = srcDataStreamRef.readInt(),
		globalTableSz = srcDataStreamRef.readInt();

	LinkerObjectFile allocLinkerDst = new LinkerObjectFile( sourceName );

	if (exportTableSz > 0)
	    for (int i = 0; i < exportTableSz; i++)
		allocLinkerDst.exportsTable.add(readExportsEntry());

	if (importTableSz > 0)
	    for (int i = 0; i < importTableSz; i++)
		allocLinkerDst.importsTable.add(readImportsEntry());

	if (typesTableSz > 0)
	    for (int i = 0; i < typesTableSz; i++)
		allocLinkerDst.typesTable.add(readTypesEntry());

	if (globalTableSz > 0)
	    for (int i = 0; i < globalTableSz; i++)
		allocLinkerDst.globalsTable.add(readGlobalsEntry());

	return allocLinkerDst;
    }

    private ExportsEntry readExportsEntry() throws IOException {
	ExportsEntry allocExportsDst = new ExportsEntry();
	allocExportsDst.typeflags = srcDataStreamRef.readByte();
	allocExportsDst.name = readString();
	allocExportsDst.labels = new LinkedList<>();
	int labelsSz = readPackedInt();
	for (int i = 0; i < labelsSz; i++)
	    allocExportsDst.labels.add(readString());
	return allocExportsDst;
    }

    private ImportsEntry readImportsEntry() throws IOException {
	ImportsEntry allocImportsDst = new ImportsEntry();
	allocImportsDst.typeflags = srcDataStreamRef.readByte();
	allocImportsDst.name = readString();

	if ((allocImportsDst.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD) == 0) {
	    int labelsSz = readPackedInt();
	    allocImportsDst.labels = new LinkedList<>();
	    for (int i = 0; i < labelsSz; i++)
		allocImportsDst.labels.add(readString());

	    if ((allocImportsDst.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
		int slotsSz = readPackedInt();
		allocImportsDst.slots = new LinkedList<>();
		for (int i = 0; i < slotsSz; i++)
		    allocImportsDst.slots.add(readPackedInt());
	    }
	}
	return allocImportsDst;
    }

    private TypesEntry readTypesEntry() throws IOException {
	TypesEntry allocTypesDst = new TypesEntry();
	allocTypesDst.slot = readPackedInt();
	allocTypesDst.typeflags = srcDataStreamRef.readByte();
	allocTypesDst.name = readString();
	if ((allocTypesDst.typeflags & LinkerObjectFile.TYPEFLAGS_TYPES_USER) != 0)
	    allocTypesDst.superName = readString();
	return allocTypesDst;
    }

    private GlobalsEntry readGlobalsEntry() throws IOException {
	GlobalsEntry allocGlobalsDst = new GlobalsEntry();
	allocGlobalsDst.type = readPackedInt();
	allocGlobalsDst.globalsflags = srcDataStreamRef.readByte();
	allocGlobalsDst.name = readString();
	return allocGlobalsDst;
    }

}
