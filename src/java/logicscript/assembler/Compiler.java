package logicscript.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.dump.AssemblyDumper;
import logicbyte.bytecode.stream.LinkerObjectOutputStream;
import logicbyte.bytecode.stream.LogicBytecodeOutputStream;
import logicbyte.bytecode.validate.BytecodeValidationException;
import logicbyte.bytecode.validate.BytecodeValidationException.IllegalOperandException;
import logicbyte.bytecode.validate.LinkerValidationException;
import logicscript.assembler.writer.ChunkAssembler;
import logicscript.ast.Chunk;
import logicscript.parser.LogicScriptParser;
import logictype.Log.BaseLog;
import logictype.Log.Level;
import logictype.Log.LogReceiverFormatted;

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
	boolean shouldDumpGeneratedBytecode = false;

	if (options.contains("+DEBUG")) {
	    System.out.println("Generating bytecode with debug symbols. Remove `+DEBUG` to generate without.");
	    compiler.setShouldGenerateDebugSymbols(true);
	}

	if (options.contains("+DUMP")) {
	    System.out.println("Dumping bytecode to dump-files for debugging. Remove `+DUMP` to disable.");
	    shouldDumpGeneratedBytecode = true;
	}

	if (options.contains("+LOG")) {
	    System.out.println("Logging is enabled. Remove `+LOG` to disable.");
	    final BaseLog logger = new BaseLog();
	    logger.addReceiver(new LogReceiverFormatted() {
		@Override
		public void tailcall(Level l, StackTraceElement[] stack, String message, Throwable cause, Object... params) {
		    PrintStream output = null;
		    switch (l) {
			case TRACE:
			case DEBUG:
			case INFO:
			    output = System.out;
			    break;
			case WARN:
			case ERR:
			    output = System.err;
			    break;
		    }
		    StackTraceElement caller = stack[0];
		    output.println(String.format("[%s][%s#%s] %s", l.name(), caller.getClassName(), caller.getMethodName(), message));
		    if (cause != null)
			cause.printStackTrace(output);
		}

	    });
	    compiler.setLog(logger);
	}

	LinkerObjectOutputStream linkerWriterObj = null;
	LogicBytecodeOutputStream bytecodeWriterObj = null;

	for (File file : srcs)
	    if (file.isFile()) {

		String baseName = file.getName();
		baseName = baseName.substring(0, baseName.lastIndexOf("."));

		System.out.print(String.format("Compiling `%s`... ", baseName));
		try {
		    fis = new FileInputStream(file);
		    dstChunkObj = new Chunk();

		    if (parser == null)
			parser = new LogicScriptParser(fis);
		    else
			parser.ReInit(fis);

		    parser.SCRIPT(dstChunkObj);
		} catch (Exception e) {
		    System.err.println(String.format("Error when parsing file `%s`:", baseName));
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
		    dstLinkerObj = new LinkerObjectFile(baseName);
		    dstBytecodeObj = new LogicBytecodeFile(baseName);
		    compiler.assembleChunk(dstChunkObj, dstLinkerObj, dstBytecodeObj, null);
		} catch (LinkerValidationException e) {
		    System.err.println(String.format("Error when compiling file `%s`:", baseName));
		    e.printStackTrace(System.err);
		} catch (BytecodeValidationException e) {
		    System.err.println(String.format("Error when compiling file `%s`:", baseName));
		    e.printStackTrace(System.err);

		    if (e instanceof IllegalOperandException) {
			IllegalOperandException ioe = (IllegalOperandException) e;
			System.err.println(String.format("\t at function `%s`, operand: %s, %s", ioe.getFunction().name(), ioe.
				getOpcodeAddress(), ioe.getOpcode()));
		    }

		} catch (Exception e) {
		    System.err.println(String.format("Error when compiling file `%s`:", baseName));
		    e.printStackTrace(System.err);
		}

		try {
		    FileOutputStream fos = null;
		    linkerWriterObj = new LinkerObjectOutputStream((OutputStream) (fos = new FileOutputStream(new File(outputDir,
			    baseName + ".o"))));
		    linkerWriterObj.writeLinkerObject(dstLinkerObj);
		    linkerWriterObj.closeWrapper();
		    linkerWriterObj = null;
		    fos.flush();
		    fos.close();

		    bytecodeWriterObj = new LogicBytecodeOutputStream((OutputStream) (fos = new FileOutputStream(new File(outputDir,
			    baseName + ".lb"))));
		    bytecodeWriterObj.writeBytecodeFile(dstBytecodeObj);
		    bytecodeWriterObj.closeWrapper();
		    bytecodeWriterObj = null;
		    fos.flush();
		    fos.close();

		    if (shouldDumpGeneratedBytecode) {
			fos = new FileOutputStream(new File(outputDir, baseName + ".od"));
			PrintWriter writer = new PrintWriter(fos);
			AssemblyDumper.dump(dstLinkerObj, writer);
			writer.flush();
			writer.close();
			fos.flush();
			fos.close();

			fos = new FileOutputStream(new File(outputDir, baseName + ".lbd"));
			writer = new PrintWriter(fos);
			AssemblyDumper.dump(dstBytecodeObj, writer);
			writer.flush();
			writer.close();
			fos.flush();
			fos.close();
		    }

		} catch (Exception e) {
		    System.err.println(String.format("Error when writing compiled linker and bytecode for file `%s`:", file));
		    e.printStackTrace(System.err);
		}
		System.out.println("OK!");
	    }
    }

}
