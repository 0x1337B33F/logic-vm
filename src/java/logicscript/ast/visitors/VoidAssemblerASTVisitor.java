package logicscript.ast.visitors;

import logicscript.ast.Chunk;
import logicscript.ast.Closure;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;

public class VoidAssemblerASTVisitor extends AbstractAssemblerASTVisitor {

    @Override
    public void visitImportDecl(LibraryBlock b) throws Exception {
	// void

    }

    @Override
    public void visitExportDecl(LibraryBlock b) throws Exception {
	// void

    }

    @Override
    public void visitGlobalsDecl(GlobalsBlock b) throws Exception {
	// void

    }

    @Override
    public void visitTypeDecl(TypeDecl t) throws Exception {
	// void

    }

    @Override
    public void onVisitChunk(Chunk c) throws Exception {
	// void

    }

    @Override
    public void onVisitChunkEnd(Chunk c) throws Exception {
	// void

    }

    @Override
    public void onVisitNFuncDecl(FunctionDecl nfunc) throws Exception {
	// void

    }

    @Override
    public void onVisitNFuncDeclEnd(FunctionDecl nfunc) throws Exception {
	// void

    }

    @Override
    public void onVisitUFuncDecl(FunctionDecl ufunc) throws Exception {
	// void

    }

    @Override
    public void onVisitUFuncDeclEnd(FunctionDecl ufunc) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncDeclLVar(FunctionLVarDecl vd) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncDeclLVarEnd(FunctionLVarDecl vd) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncDeclBlock(Block b) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncDeclBlockEnd(Block b) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncBlock(Block b) throws Exception {
	// void

    }

    @Override
    public void onVisitFuncBlockEnd(Block b) throws Exception {
	// void

    }

    @Override
    public void onVisitClosure(Closure c) throws Exception {
	// void
    }

    @Override
    public void onVisitClosureEnd(Closure c) throws Exception {
	// void
    }

    @Override
    public void onVisitStatement(Statement s) throws Exception {
	// void

    }

    @Override
    public void onVisitStatementEnd(Statement s) throws Exception {
	// void

    }

    @Override
    public void onVisitExpression(Expression e) throws Exception {
	// void

    }

    @Override
    public void onVisitExpressionEnd(Expression e) throws Exception {
	// void

    }

}
