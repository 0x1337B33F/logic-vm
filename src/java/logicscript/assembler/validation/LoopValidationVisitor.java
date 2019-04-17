package logicscript.assembler.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import logicscript.ast.visitors.VoidAssemblerASTVisitor;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;
import logicscript.ast.nodes.stmt.cfl.ExitWhenStatement;
import logicscript.ast.nodes.stmt.cfl.ForStatement;
import logicscript.ast.nodes.stmt.cfl.IfStatement;
import logicscript.ast.nodes.stmt.cfl.LoopStatement;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;
import logicscript.ast.nodes.stmt.cfl.TryCatchStatement;
import logicscript.ast.nodes.stmt.ef.DebugStatement;

/**
 *
 * @author Lochie
 */
public class LoopValidationVisitor extends VoidAssemblerASTVisitor {

    public static class LoopExitPathInfo {

	public FunctionDecl funcDecl;
	public LoopStatement loopBlock;
	public List<ExitWhenStatement> exitStatements;
	public List<ReturnStatement> returnStatements;
    }

    Map<FunctionDecl, List<LoopExitPathInfo>> functionLoopStatements;

    private volatile Chunk generatingChunk = null;
    private volatile FunctionDecl activeFunction = null;

    public Map<FunctionDecl, List<LoopExitPathInfo>> getFunctionLoopsTable() {
	return functionLoopStatements;
    }

    @Override
    public void onVisitChunk(Chunk c) throws Exception {
	this.generatingChunk = c;
	this.functionLoopStatements = new HashMap<>();
    }

    @Override
    public void onVisitChunkEnd(Chunk c) throws Exception {
	this.generatingChunk = null;
    }

    @Override
    public void onVisitUFuncDecl(FunctionDecl ufunc) throws Exception {
	this.activeFunction = ufunc;
    }

    @Override
    public void onVisitUFuncDeclEnd(FunctionDecl ufunc) throws Exception {
	this.activeFunction = null;
    }

    @Override
    public void onVisitStatement(Statement s) throws Exception {

	if (this.activeFunction == null)
	    return;

	if (s instanceof LoopStatement) {
	    LoopStatement loop = (LoopStatement) s;

	    LoopExitPathInfo struct = new LoopExitPathInfo();
	    struct.funcDecl = activeFunction;
	    struct.loopBlock = loop;

	    List<LoopExitPathInfo> ptr = functionLoopStatements.get(activeFunction);
	    if (ptr == null)
		functionLoopStatements.put(activeFunction, ptr = new LinkedList<>());
	    ptr.add(struct);

	    List<ExitWhenStatement> exitSet = new LinkedList<>();
	    List<ReturnStatement> returnSet = new LinkedList<>();

	    /**
	     * check that at least one EXITWHEN statement is defined, even if it is potentially impossible to ever arrive at it:
	     */
	    if (!checkLoopContainsExit(loop.statements, exitSet, returnSet))
		throw new Exception(String.format("LOOP statement does not contain any EXITWHEN statements (at %s)", loop.location));

	    struct.exitStatements = exitSet;
	    struct.returnStatements = returnSet;
	}
    }

    public boolean checkLoopContainsExit(Block block, List<ExitWhenStatement> exitList, List<ReturnStatement> returnList) {

	if (block == null || block.isEmpty())
	    return false; // clearly not

	boolean flag = false;

	Iterator<Statement> itx = block.iterator();
	while (itx.hasNext()) {
	    Statement stmt = itx.next();

	    if (stmt instanceof DebugStatement)
		stmt = ((DebugStatement) stmt).statement; // inspect inner statement

	    if (stmt instanceof ReturnStatement) {
		returnList.add((ReturnStatement) stmt);
		flag = true;
	    } else if (stmt instanceof ExitWhenStatement) {
		exitList.add((ExitWhenStatement) stmt);
		flag = true;
	    } else if (stmt instanceof IfStatement) {
		IfStatement ifCompound = (IfStatement) stmt;
		if (ifCompound.thenBlock != null)
		    if (checkLoopContainsExit(ifCompound.thenBlock, exitList, returnList))
			flag = true;
		if (ifCompound.elseIfBlocks != null) {
		    Iterator<Block> itx1 = ifCompound.elseIfBlocks.iterator();
		    while (itx1.hasNext())
			if (checkLoopContainsExit(itx1.next(), exitList, returnList))
			    flag = true;
		}
		if (ifCompound.elseBlock != null)
		    if (checkLoopContainsExit(ifCompound.elseBlock, exitList, returnList))
			flag = true;
	    } else if (stmt instanceof LoopStatement) {
		/**
		 * don't descend into the inner loop, the EXITWHEN within the nested loop may only refer to the current loop; the inner loop
		 * will be checked when it arrives at onVisitStatement()
		 */
	    } else if (stmt instanceof ForStatement) {
		ForStatement forCompound = (ForStatement) stmt;
		if (forCompound.thenBlock != null)
		    if (checkLoopContainsExit(forCompound.thenBlock, exitList, returnList))
			flag = true;
	    } else if (stmt instanceof TryCatchStatement) {
		TryCatchStatement tryCatchCompound = (TryCatchStatement) stmt;
		if (tryCatchCompound.tryBlock != null)
		    if (checkLoopContainsExit(tryCatchCompound.tryBlock, exitList, returnList))
			flag = true;
		if (tryCatchCompound.catchBlocks != null) {
		    Iterator<TryCatchStatement.CatchBlock> itx1 = tryCatchCompound.catchBlocks.iterator();
		    while (itx1.hasNext())
			if (checkLoopContainsExit(itx1.next().block, exitList, returnList))
			    flag = true;
		}
	    }
	}
	return flag;
    }

}
