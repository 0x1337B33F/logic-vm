package logicscript.assembler;

import java.util.List;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDeclMutable;
import logictype.Log;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public interface FunctionBytecodeTransformer {

    public void apply(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, Log log) throws Exception;

}
