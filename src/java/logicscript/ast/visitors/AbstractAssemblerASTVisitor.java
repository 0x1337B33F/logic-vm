package logicscript.ast.visitors;

import java.util.LinkedList;
import logicscript.ast.Chunk;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.Closure;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;

public abstract class AbstractAssemblerASTVisitor implements ChunkVisitor {

    protected LinkedList<Closure> closureStack = new LinkedList<>();

    public abstract void onVisitChunk(Chunk c) throws Exception;

    public abstract void onVisitChunkEnd(Chunk c) throws Exception;

    @Override
    public void visitChunk(Chunk c) throws Exception {
	onVisitChunk(c);
	c.visit(this);
	onVisitChunkEnd(c);
    }

    public abstract void onVisitNFuncDecl(FunctionDecl nfunc) throws Exception;

    public abstract void onVisitNFuncDeclEnd(FunctionDecl nfunc) throws Exception;

    @Override
    public void visitNFuncDecl(FunctionDecl nfunc) throws Exception {
	onVisitNFuncDecl(nfunc);
	nfunc.visit(this);
	onVisitNFuncDeclEnd(nfunc);
    }

    public abstract void onVisitUFuncDecl(FunctionDecl ufunc) throws Exception;

    public abstract void onVisitUFuncDeclEnd(FunctionDecl ufunc) throws Exception;

    @Override
    public void visitUFuncDecl(FunctionDecl ufunc) throws Exception {
	onVisitUFuncDecl(ufunc);
	ufunc.visit(this);
	onVisitUFuncDeclEnd(ufunc);
    }

    public abstract void onVisitFuncDeclLVar(FunctionLVarDecl vd) throws Exception;

    public abstract void onVisitFuncDeclLVarEnd(FunctionLVarDecl vd) throws Exception;

    @Override
    public void visitFuncDeclLVar(FunctionLVarDecl vd) throws Exception {
	onVisitFuncDeclLVar(vd);
	vd.visit(this);
	onVisitFuncDeclLVarEnd(vd);
    }

    public abstract void onVisitFuncDeclBlock(Block b) throws Exception;

    public abstract void onVisitFuncDeclBlockEnd(Block b) throws Exception;

    @Override
    public void visitFuncDeclBlock(Block b) throws Exception {
	onVisitFuncDeclBlock(b);
	b.visit(this);
	onVisitFuncDeclBlockEnd(b);
    }

    public abstract void onVisitFuncBlock(Block b) throws Exception;

    public abstract void onVisitFuncBlockEnd(Block b) throws Exception;

    @Override
    public void visitFuncBlock(Block b) throws Exception {
	onVisitFuncBlock(b);
	b.visit(this);
	onVisitFuncBlockEnd(b);
    }

    /**
     * Called when a closure is visited, after the closure has been placed on the stack.
     *
     * @param c
     *
     * @throws Exception
     */
    public abstract void onVisitClosure(Closure c) throws Exception;

    /**
     * Called after a closure is exited, after the closure has been removed from the stack.
     *
     * @param c
     *
     * @throws Exception
     */
    public abstract void onVisitClosureEnd(Closure c) throws Exception;

    @Override
    public void visitClosure(Closure c) throws Exception {
	this.closureStack.push(c);
	this.onVisitClosure(c);
    }

    @Override
    public void visitClosureEnd(Closure c) throws Exception {
	if (this.closureStack.pop() != c)
	    throw new Exception("Closure stack under/overflow");
	this.onVisitClosureEnd(c);
    }

    public abstract void onVisitStatement(Statement s) throws Exception;

    public abstract void onVisitStatementEnd(Statement s) throws Exception;

    @Override
    public void visitStatement(Statement s) throws Exception {
	onVisitStatement(s);
	s.visit(this);
	onVisitStatementEnd(s);
    }

    public abstract void onVisitExpression(Expression e) throws Exception;

    public abstract void onVisitExpressionEnd(Expression e) throws Exception;

    @Override
    public void visitExpression(Expression e) throws Exception {
	onVisitExpression(e);
	e.visit(this);
	onVisitExpressionEnd(e);
    }
}
