package logicscript.assembler.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import logicscript.assembler.ExpressionResolver;
import logicscript.ast.visitors.VoidAssemblerASTVisitor;
import logicscript.ast.Chunk;
import logicscript.ast.Closure;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;
import logicscript.ast.nodes.stmt.cfl.ForStatement;
import logicscript.ast.nodes.stmt.cfl.IfStatement;
import logicscript.ast.nodes.stmt.cfl.LoopStatement;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;
import logicscript.ast.nodes.stmt.cfl.TryCatchStatement;
import logicscript.ast.nodes.stmt.ef.DebugStatement;

public class ReturnValidationVisitor extends VoidAssemblerASTVisitor {

    public static class FunctionReturnPathInfo {

	public FunctionDecl funcDecl;
	public List<ReturnStatement> returnStatements;
    }

    Map<FunctionDecl, FunctionReturnPathInfo> functionReturnsTable;

    volatile Chunk generatingChunk = null;

    /**
     * <p>
     * Get the last generated function return path table. The return path table is a hash-indexed Map whose keys are the function
     * declarations themselves. The struct value contains the function declaration and a List containing all ReturnStatement node(s), or an
     * empty list if the function does not specify any RETURNS statement (implicit <code>nothing</code> case only).</p>
     *
     *
     * <p>
     * The List may include RETURNS statements which are unreachable. The List is ordered in flattened tree left-to-right order.</p>
     *
     * @return Unsynchronized access to the function return path info Map
     */
    public Map<FunctionDecl, FunctionReturnPathInfo> getFunctionReturnPathTable() {
	return functionReturnsTable;
    }

    @Override
    public void onVisitChunk(Chunk c) throws Exception {
	this.generatingChunk = c;
	this.functionReturnsTable = new HashMap<>();
    }

    @Override
    public void onVisitChunkEnd(Chunk c) throws Exception {
	this.generatingChunk = null;
    }

    @Override
    public void visitUFuncDecl(FunctionDecl ufunc) throws Exception {
	FunctionReturnPathInfo struct = new FunctionReturnPathInfo();
	struct.funcDecl = ufunc;
	functionReturnsTable.put(ufunc, struct);

	List<ReturnStatement> returnSet = new LinkedList();
	if (!checkAllCodePathsHaveReturn(generatingChunk, ufunc, closureStack, ufunc.block, returnSet))
	    if (ufunc.returnType != null && ufunc.returnType != TypeDecl.TYPE_NOTHING)
		throw new Exception(String.format("Not all code path(s) return a value at function %s (at %s)", ufunc.name,
			ufunc.location));
	struct.returnStatements = returnSet;

    }

    /**
     * Confirm a function declaration contains all required RETURNS statements and that any implemented RETURNS expression(s) result in a
     * value of the return type specified.
     *
     *
     * @param originChunk  The script chunk which defined the user function
     * @param decl         The function declaration
     * @param closureStack The current closure stack
     * @param block        The current block (either the funcdecl block or a nested block)
     * @param returnSet    The list which shall hold all the RETURNS statement reference(s) encountered
     *
     * @return Whether the block is CLOSED (ie, ends with a RETURNS statement) or OPEN (the block contains no RETURNS); from invocation on
     *         the funcdecl block, this indicates whether all code paths shall return a value; from invocation on a nested block, this
     *         indicates whether the specific block (and any block(s) nested within the block) is CLOSED or OPEN.
     *
     * @throws Exception If the RETURNS statement expression shall generate a value whose type does not match the return-type of the
     *                   function declaration, an exception shall be thrown; if the block contains unreachable statements (those after a
     *                   RETURNS statement), an exception shall be thrown.
     */
    protected boolean checkAllCodePathsHaveReturn(Chunk originChunk, FunctionDecl decl, List<Closure> closureStack, Block block,
	    List<ReturnStatement> returnSet) throws Exception {
	boolean flag0 = (decl.block == block), /* if the block is the funcdecl block */
		flag1 = false,/* if the block has been closed by a RETURN word */
		flag2 = false;/* if the current block contains a block which is not closed */

	TypeDecl reqReturnType = (decl.returnType != null) ? decl.returnType : TypeDecl.TYPE_NOTHING;

	if (block.isEmpty())
	    flag2 = true;
	/* the block itself is not closing if it is empty! */
	else {
	    Iterator<Statement> itx = block.iterator();
	    while (itx.hasNext()) {
		Statement stmt = itx.next();

		if (stmt instanceof ReturnStatement) {
		    flag1 = true;
		    TypeDecl expectedResultType = (((ReturnStatement) stmt).returnExpression == null) ? TypeDecl.TYPE_NOTHING
			    : ExpressionResolver.computeExpressionResultType(originChunk, decl, closureStack,
				    ((ReturnStatement) stmt).returnExpression);
		    if (expectedResultType != null && expectedResultType != reqReturnType)
			throw new Exception(String.format("Expected %s return, got %s (at %s)", reqReturnType, expectedResultType,
				((ReturnStatement) stmt).returnExpression.location));
		    returnSet.add((ReturnStatement) stmt);
		    continue;
		}

		/* already closed ? */
		if (flag1)
		    throw new Exception(String.format("Unreachable code %s (at %s)", stmt, stmt.location));

		if (stmt instanceof DebugStatement)
		    /* Inspect the nested statement: */
		    stmt = ((DebugStatement) stmt).statement;

		if (stmt instanceof IfStatement) {
		    IfStatement ifCompound = (IfStatement) stmt;
		    if (ifCompound.thenBlock != null)
			if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, ifCompound.thenBlock, returnSet))
			    flag2 = true;

		    if (ifCompound.elseIfBlocks != null) {
			Iterator<Block> itx1 = ifCompound.elseIfBlocks.iterator();
			while (itx1.hasNext())
			    if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, itx1.next(), returnSet))
				flag2 = true;
		    }

		    if (ifCompound.elseBlock != null)
			if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, ifCompound.elseBlock, returnSet))
			    flag2 = true;
		} else if (stmt instanceof ForStatement) {
		    ForStatement forCompound = (ForStatement) stmt;
		    if (forCompound.thenBlock != null)
			if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, forCompound.thenBlock, returnSet))
			    flag2 = true;
		} else if (stmt instanceof TryCatchStatement) {
		    TryCatchStatement tryCatchCompound = (TryCatchStatement) stmt;
		    if (tryCatchCompound.tryBlock != null)
			if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, tryCatchCompound.tryBlock, returnSet))
			    flag2 = true;

		    if (tryCatchCompound.catchBlocks != null) {
			Iterator<TryCatchStatement.CatchBlock> itx1 = tryCatchCompound.catchBlocks.iterator();
			while (itx1.hasNext())
			    if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, itx1.next().block, returnSet))
				flag2 = true;
		    }

		} else if (stmt instanceof LoopStatement) {
		    LoopStatement loopCompound = (LoopStatement) stmt;
		    if (loopCompound.statements != null)
			if (!checkAllCodePathsHaveReturn(originChunk, decl, closureStack, loopCompound.statements, returnSet))
			    flag2 = true;
		}
	    }
	}

	/* return if has final returns or has no open nested nodes;
	 * if all nested nodes are closed, then all nodes and children of nodes
	 * explicitly (or somewhere) have RETURNS within them. */
	return flag1 || !flag2;
    }

}
