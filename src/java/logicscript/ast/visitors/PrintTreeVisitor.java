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

public class PrintTreeVisitor extends AbstractAssemblerASTVisitor {

    int printLevel = 0;

    String t() {
	if (printLevel <= 0)
	    return "";
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < printLevel; i++)
	    sb.append("  ");
	return sb.toString();
    }

    void printf(String pattern, Object... args) {
	System.out.println(String.format("%s%s", t(), String.format(pattern, args)));

    }

    @Override
    public void visitImportDecl(LibraryBlock b) {
	printf("ImportsDeclarationBlock: %s", b);
    }

    @Override
    public void visitExportDecl(LibraryBlock b) {
	printf("ExportsDeclarationBlock: %s", b);
    }

    @Override
    public void visitGlobalsDecl(GlobalsBlock b) {
	printf("GlobalsDeclarationBlock: %s", b);
    }

    @Override
    public void visitTypeDecl(TypeDecl t) {
	printf("TypeDecl: (%s): %s => %s, %s", t.type, t.typeName, t.ancestorTypeName, t.location);
    }

    @Override
    public void onVisitChunk(Chunk c) {
	printf("Chunk: %s", c);
	printLevel++;
    }

    @Override
    public void onVisitNFuncDecl(FunctionDecl nfunc) {
	printf("NativeFunctionDecl: %s =>> %s (at %s)", nfunc.name, nfunc.returnType, nfunc.location);
	printLevel++;
    }

    @Override
    public void onVisitUFuncDecl(FunctionDecl ufunc) {
	printf("UserFunctionDecl: %s =>> %s (at %s)", ufunc.name, ufunc.returnType, ufunc.location);
	printLevel++;
    }

    @Override
    public void onVisitFuncDeclLVar(FunctionLVarDecl vd) {
	printf("UserFunctionLocalVarDecl: %s (type %s, %s) (at %s)", vd.nameOfLocalVar, vd.valueType, ( vd.isArrayType ) ? "array" : "value", vd.location);
	printLevel++;
    }

    @Override
    public void onVisitFuncDeclBlock(Block b) {
	printf("FuncDeclBlock: %s", b);
	printLevel++;
    }

    @Override
    public void onVisitFuncBlock(Block b) {
	printf("Block: %s", b);
	printLevel++;
    }

    @Override
    public void onVisitClosure(Closure c) throws Exception {
	printf("Closure: %s (defines %s vars) (at %s)", c, c.numcvars());
	printLevel++;
    }

    @Override
    public void onVisitClosureEnd(Closure c) throws Exception {
	printf("(endclosure)");
	printLevel--;
    }

    @Override
    public void onVisitStatement(Statement s) {
	printf("Statement: %s (at %s)", s, s.location);
	printLevel++;
    }

    @Override
    public void onVisitExpression(Expression e) {
	printf("Expression: %s (at %s)", e, e.location);
	printLevel++;
    }

    @Override
    public void onVisitChunkEnd(Chunk c) {
	printLevel--;
    }

    @Override
    public void onVisitNFuncDeclEnd(FunctionDecl nfunc) {
	printLevel--;
    }

    @Override
    public void onVisitUFuncDeclEnd(FunctionDecl ufunc) {
	printLevel--;
    }

    @Override
    public void onVisitFuncDeclLVarEnd(FunctionLVarDecl vd) {
	printLevel--;
    }

    @Override
    public void onVisitFuncDeclBlockEnd(Block b) {
	printLevel--;
    }

    @Override
    public void onVisitFuncBlockEnd(Block b) {
	printLevel--;
    }

    @Override
    public void onVisitStatementEnd(Statement s) {
	printLevel--;
    }

    @Override
    public void onVisitExpressionEnd(Expression e) {
	printLevel--;
    }

}
