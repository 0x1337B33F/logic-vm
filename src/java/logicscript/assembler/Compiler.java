package logicscript.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.stream.LinkerObjectOutputStream;
import logicbyte.bytecode.stream.LogicBytecodeOutputStream;
import logicscript.assembler.writer.ChunkAssembler;
import logicscript.ast.Chunk;
import logicscript.parser.LogicScriptParser;

/**
 *
 * @author Lochie
 */
public class Compiler {

    public static void main(String[] args) {
	try {
	    (new Compiler(args)).run();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    public final String[] args;

    public File workingDir = null;
    public File outputDir = null;

    public List<String> options = new LinkedList<>();

    protected Compiler(String[] args) {
	this.args = args;
    }

    protected void run() throws Exception {
	parseargs();
	generate();
    }

    protected void parseargs() throws Exception {
	for (int i = 0; i < args.length;) {
	    String data = args[i];
	    if (data.startsWith("-")) {
		data = data.substring(1);
		String val = args[i + 1];

		if (data.equalsIgnoreCase("source")) {
		    File f = new File(val);
		    if (!f.exists() || !f.isDirectory())
			throw new Exception("Source directory not a directory");
		    workingDir = f;
		}

		if (data.equalsIgnoreCase("dest")) {
		    File f = new File(val);
		    if (!f.exists() || !f.isDirectory())
			throw new Exception("Destination directory not a directory");
		    outputDir = f;
		}
		i += 2;
	    } else {
		options.add(data);
		i++;
	    }
	}
    }

    protected void generate() {

	System.out.println(String.format("Compiling from `%s` to `%s`...", workingDir, outputDir));

	File[] srcs = workingDir.listFiles();

	FileInputStream fis = null;
	LogicScriptParser parser = null;
	Chunk dstChunkObj = null;

	ChunkAssembler compiler = new ChunkAssembler();
	LinkerObjectFile dstLinkerObj = null;
	LogicBytecodeFile dstBytecodeObj = null;

	if (options.contains("+DEBUG")) {
	    System.out.println("Generating bytecode with debug symbols. Remove `+DEBUG` to generate without.");
	    compiler.setShouldGenerateDebugSymbols(true);
	}

	LinkerObjectOutputStream linkerWriterObj = null;
	LogicBytecodeOutputStream bytecodeWriterObj = null;

	for (File file : srcs)
	    if (file.isFile()) {
		System.out.print(String.format("Compiling `%s`... ", file.getName()));
		try {
		    fis = new FileInputStream(file);
		    dstChunkObj = new Chunk();

		    if (parser == null)
			parser = new LogicScriptParser(fis);
		    else
			parser.ReInit(fis);

		    parser.SCRIPT(dstChunkObj);
		} catch (Exception e) {
		    System.err.println(String.format("Error when parsing file `%s`:", file));
		    e.printStackTrace(System.err);
		    continue;
		} finally {
		    if (fis != null)
			try {
			    fis.close();
			} catch (Exception e) {
			}
		}

		try {
		    dstLinkerObj = new LinkerObjectFile();
		    dstBytecodeObj = new LogicBytecodeFile();
		    compiler.assembleChunk(dstChunkObj, dstLinkerObj, dstBytecodeObj);
		} catch (Exception e) {
		    System.err.println(String.format("Error when compiling file `%s`:", file));
		    e.printStackTrace(System.err);
		}

		try {
		    FileOutputStream fos = null;
		    linkerWriterObj = new LinkerObjectOutputStream((OutputStream) (fos = new FileOutputStream(new File(outputDir, file.
			    getName() + ".o"))));
		    linkerWriterObj.writeLinkerObject(dstLinkerObj);
		    linkerWriterObj.closeWrapper();
		    linkerWriterObj = null;
		    fos.flush();
		    fos.close();

		    bytecodeWriterObj = new LogicBytecodeOutputStream((OutputStream) (fos = new FileOutputStream(new File(outputDir, file.
			    getName() + ".lb"))));
		    bytecodeWriterObj.writeBytecodeFile(dstBytecodeObj);
		    bytecodeWriterObj.closeWrapper();
		    bytecodeWriterObj = null;
		    fos.flush();
		    fos.close();
		} catch (Exception e) {
		    System.err.println(String.format("Error when writing compiled linker and bytecode for file `%s`:", file));
		    e.printStackTrace(System.err);
		}
		System.out.println("OK!");
	    }
    }

}
