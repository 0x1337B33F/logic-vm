package logicscript.ast;

import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;

/**
 * Contract interface for classes which implement LogicScript Chunk (script-file) abstract-syntax-tree visitors. The methods on the visitor
 * are called recursively; if an object is received by the visitor implementation, it may choose to receive additional invocations for
 * nested node(s) by invoking the {@link Visitable#visit(logicscript.ast.ChunkVisitor)} method on the received node.
 *
 * @author Lochie
 */
public interface ChunkVisitor {

    /**
     * Visits the passed Chunk object. Receiver may access all properties of the Chunk object.
     *
     * Receiver may continue descent (ImportDecl, ExportDecl, GlobalsDecl, TypeDecl, NFuncDecl, UFuncDecl) by continuing with
     * {@link Visitable#visit(logicscript.ast.ChunkVisitor)}
     *
     * @param c
     */
    public void visitChunk(Chunk c) throws Exception;

    public void visitImportDecl(LibraryBlock b) throws Exception;

    public void visitExportDecl(LibraryBlock b) throws Exception;

    public void visitGlobalsDecl(GlobalsBlock b) throws Exception;

    public void visitTypeDecl(TypeDecl t) throws Exception;

    /**
     * Visits the passed native function declaration.
     *
     * @param nfunc
     */
    public void visitNFuncDecl(FunctionDecl nfunc) throws Exception;

    /**
     * Visits the passed user function declaration.
     *
     * Receiver may continue descent (FuncDeclLVar, FuncDeclBlock) by continuing with {@link Visitable#visit(logicscript.ast.ChunkVisitor)}
     *
     * @param ufunc
     */
    public void visitUFuncDecl(FunctionDecl ufunc) throws Exception;

    /* -> Chunk -> FunctionDecl -> */
    public void visitFuncDeclLVar(FunctionLVarDecl vd) throws Exception;

    /**
     * Visits the passed top-level function block.
     *
     * @param b
     */
    public void visitFuncDeclBlock(Block b) throws Exception;

    /**
     * Visits any nested function block(s), such as those used by control-flow structures.
     *
     * @param b
     */
    public void visitFuncBlock(Block b) throws Exception;

    /**
     * Visits an AST node which is a Closure; it implements local variables within its effective scope.
     *
     * @param c
     *
     * @throws Exception
     */
    public void visitClosure(Closure c) throws Exception;

    public void visitClosureEnd(Closure c) throws Exception;

    /* -> Block -> */
    public void visitStatement(Statement s) throws Exception;

    /* -> Expression -> */
    public void visitExpression(Expression e) throws Exception;

}
