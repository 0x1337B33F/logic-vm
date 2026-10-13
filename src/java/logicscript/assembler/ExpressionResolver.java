package logicscript.assembler;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.Chunk;
import logicscript.ast.Closure;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.BinaryExpression;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.expr.FunctionCallExpression;
import logicscript.ast.nodes.expr.FunctionCallVarExpression;
import logicscript.ast.nodes.expr.FunctionRefExpression;
import logicscript.ast.nodes.expr.LiteralValue;
import logicscript.ast.nodes.expr.ParenExpression;
import logicscript.ast.nodes.expr.UnaryExpression;
import logicscript.ast.nodes.expr.VariableRefExpression;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.Statement;
import logicscript.ast.nodes.stmt.cfl.IfStatement;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;

public class ExpressionResolver {

    public static class TypeMismatchException extends Exception {

	public TypeMismatchException(TypeDecl a, TypeDecl b, Expression cause) {
	    super(String.format("Type mismatch; expecting %s, got %s (at %s)", a, b, cause.location));
	}
    }

    /**
     * <p>
     * Introspect the defining block of the function declaration provided to determine if a specific LiteralValue can be generated
     * consistently for the provided parameter value(s). Depending on the structure of the function, a variety of simplifications shall
     * occur:
     * </p>
     *
     * <ul>
     * <li>The function shall be checked to see if it contains only a <code>return</code> with a resolve-able LiteralValue: in this case,
     * the function shall consistently return the expression result;</li>
     * <li>The function shall be checked to see if the control-flow logic can be eliminated to a single state (by first quickly evaluating
     * all lvar expressions) to assert if a specific LiteralValue is returned by a specific set of input parameter(s) (such as for if then
     * return else return end, etc)</li>
     * </ul>
     *
     * <p>
     * The simplification of the function specifically implies that for the given parameters (or none), the function returns a constant
     * literal value which can be pre-computed (either from expressions which may involve constant globals, local variables with constant
     * values or which resolve to LiteralValue) fully, and which does not depend on function expression evaluation or which contains
     * local-modifying operations.
     * </p>
     *
     * @param originChunk
     * @param function
     * @param closureStack
     *
     * @return
     *
     * @throws Exception
     */
    public static Expression simplifyFunctionReturn(Chunk originChunk, FunctionDecl function, List<Closure> closureStack) throws Exception {

	/*
	* simplification of user pointers, java object pointers & nothing is not
	* permitted:
	 */
	if (function.returnType == TypeDecl.TYPE_CODE || function.returnType == TypeDecl.TYPE_HANDLE
		|| function.returnType == TypeDecl.TYPE_NOTHING)
	    return null;

	/*
	 * The function return shall now be one of BOOLEAN, INTEGER, REAL or STRING...
	 */
	Block functionDeclBlock = function.block;
	/* If the function body is totally empty... */
	if (functionDeclBlock.isEmpty())
	    /* The decl is probably technically invalid, but indicate the return type would actually be `nothing` for now */
	    return new LiteralValue(TypeDecl.TYPE_NOTHING, null);

	/* 
	* The function body can contain three types of statements which makes them simplifiable:
	* - a single RETURN or an IFTHEN compound (where all nodes of IFTHEN contain RETURNS); or
	* - an IFTHEN compound (where not all nodes of IFTHEN contain RETURNS) followed by a 'default' RETURNS.
	 */
	if (functionDeclBlock.size() == 1 || functionDeclBlock.size() == 2) {
	    logicscript.ast.nodes.stmt.Statement stmt0 = functionDeclBlock.get(0),
		    stmt1 = (functionDeclBlock.size() == 2) ? functionDeclBlock.get(1) : null;

	    /* If it is an explicit `function ... returns t return expr end`... */
	    if (stmt0 instanceof ReturnStatement) {
		ReturnStatement singleReturn = (ReturnStatement) stmt0;
		Expression resultOfSimplify = (singleReturn.returnExpression instanceof LiteralValue) ? singleReturn.returnExpression
			: simplifyExpressionTree(originChunk, function, closureStack, singleReturn.returnExpression);

		/* ... and the RETURN expression can be simplified to a literal... */
		if (resultOfSimplify instanceof LiteralValue)
		    /* ... then, indeed, we can assert the result the return of the function. */
		    return resultOfSimplify;
		else
		    /* ... else the RETURN contains unsimplifable term(s) */
		    return null;
	    }

	    /*
	     * If it is a logical expansion, based on params (`function complex returns t if
	     * cond then return a elseif (compound) else return z end`)
	     */
	    if (stmt0 instanceof IfStatement) {
		/*
		* let us begin stepping through the if-elseif-else compound: the value of all
		* variables being either known or unknown, determined by the resolver if the
		* attached matching block has only return, it also satisfies criterion for
		* simplification since the expression providing the JUMP to the block would
		* have been matched anyway.
		 */
		IfStatement ifCompound = (IfStatement) stmt0;

		Boolean flag = null;
		Expression resultOfIfTest = (ifCompound.conditionalExpr instanceof LiteralValue) ? ifCompound.conditionalExpr
			: simplifyExpressionTree(originChunk, function, closureStack, ifCompound.conditionalExpr);

		/* If evaulation of the condition results in an LV: */
		if (resultOfIfTest instanceof LiteralValue) {
		    LiteralValue ifTestResult = (LiteralValue) resultOfIfTest;
		    /* If the LV is a boolean: */
		    if (ifTestResult.typeOfValue == TypeDecl.TYPE_BOOLEAN)
			flag = (Boolean) ifTestResult.valueObject;
		    /* Expand the LV of the condexpr */
		}

		/* If the LV of the condexpr is true, then: */
		if (flag != null && flag) {
		    Block thenBlock = ifCompound.thenBlock;
		    /*
		    * If the TRUE block of the condexpr is empty, then consider the
		    * second top-level statement (if any):
		     */
		    if (thenBlock.size() == 0 && stmt1 == null)
			return null;
		    if (thenBlock.size() == 1) {
			Statement stmt2 = thenBlock.get(0);

			/* If the only statement in the THEN block is a RETURN... */
			if (stmt2 instanceof ReturnStatement) {
			    ReturnStatement singleReturn = (ReturnStatement) stmt2;
			    Expression resultOfSimplify = (singleReturn.returnExpression instanceof LiteralValue) ? singleReturn.returnExpression
				    : simplifyExpressionTree(originChunk, function, closureStack, singleReturn.returnExpression);

			    /* ... and the RETURN expression can be simplified to a literal... */
			    if (resultOfSimplify instanceof LiteralValue)
				/* ... then, indeed, we can assert the result the return of the function. */
				return resultOfSimplify;
			    else
				/* ... else the RETURN contains unsimplifable term(s) */
				return null;
			} else
			    return null;
		    } else
			return null;
		}

		/*
		* If the LV of the condexpr is false, then one of the following applies:
		* - If there are elseif blocks, step each elseif block; or
		* - If there is an else block, it is the else block.
		 */
		if (flag != null && !flag)
		    if (ifCompound.elseIfBlocks != null && ifCompound.elseIfBlocks.size() > 0) {
			Integer idx = null;
			for (int i = 0, j = ifCompound.elseIfCondExprs.size(); i < j; i++) {
			    Expression elseIfExpr = ifCompound.elseIfCondExprs.get(i),
				    resultOfElseIfExpr = (elseIfExpr instanceof LiteralValue) ? elseIfExpr
					    : simplifyExpressionTree(originChunk, function, closureStack, elseIfExpr);

			    /* If the evaluation of the elseif condition results in an LV: */
			    if (resultOfElseIfExpr instanceof LiteralValue) {
				LiteralValue elseIfTestResult = (LiteralValue) resultOfElseIfExpr;
				/* If the LV result is a boolean: */
				if (elseIfTestResult.typeOfValue == TypeDecl.TYPE_BOOLEAN && (Boolean) elseIfTestResult.valueObject) {
				    /* Record which index of elseif we are in: */
				    idx = i;
				    break;
				}
			    }
			}

			/* If an elseif was found wich has LV true: */
			if (idx != null) {
			    Block elseIfBlock = ifCompound.elseIfBlocks.get(idx);

			    /* If the block is empty, return nothing: */
			    if (elseIfBlock.size() == 0)
				return null;

			    /* If the block has one statement: */
			    if (elseIfBlock.size() == 1) {
				stmt0 = elseIfBlock.get(0);

				/* If the one statement is a RETURN: */
				if (stmt0 instanceof ReturnStatement) {
				    ReturnStatement elseIfRetn = (ReturnStatement) stmt0;
				    Expression resultOfSimplify = (elseIfRetn.returnExpression instanceof LiteralValue) ? elseIfRetn.returnExpression
					    : simplifyExpressionTree(originChunk, function, closureStack, elseIfRetn.returnExpression);

				    /* ... and the RETURN expression can be simplified to a literal... */
				    if (resultOfSimplify instanceof LiteralValue)
					/* ... then, indeed, we can assert the result the return of the function. */
					return resultOfSimplify;
				    else
					/* ... else the RETURN contains unsimplifable term(s) */
					return null;
				} else
				    return null;
			    } else
				return null;

			}
		    }

		if (flag != null && !flag) {
		    Block elseBlock = ifCompound.elseBlock;
		    if (elseBlock.size() == 0 && stmt1 == null)
			return null;
		    if (elseBlock.size() == 1) {
			stmt0 = elseBlock.get(0);

			/* If the one statement is a RETURN: */
			if (stmt0 instanceof ReturnStatement) {
			    ReturnStatement elseIfRetn = (ReturnStatement) stmt0;
			    Expression resultOfSimplify = (elseIfRetn.returnExpression instanceof LiteralValue) ? elseIfRetn.returnExpression
				    : simplifyExpressionTree(originChunk, function, closureStack, elseIfRetn.returnExpression);

			    /* ... and the RETURN expression can be simplified to a literal... */
			    if (resultOfSimplify instanceof LiteralValue)
				/* ... then, indeed, we can assert the result the return of the function. */
				return resultOfSimplify;
			    else
				/* ... else the RETURN contains unsimplifable term(s) */
				return null;
			} else
			    return null;
		    }

		}
		/* 
		* If we have evaluated the IF-THEN, and the ELSEIF blocks, then
		* the only remaining option is the ELSE block:
		 */
	    }

	    /* Else the function cannot be easily simplified (yet) */
	    return null;
	} else {

	    /*
	    * If the function body has more than one statement, establish if the statements
	    * are also certain type(s) only:
	     */
	    boolean flag = false;
	    Iterator<Statement> itx = functionDeclBlock.iterator();
	    while (itx.hasNext()) {
		Statement stmt1 = itx.next();
		if (!((stmt1 instanceof ReturnStatement) || (stmt1 instanceof IfStatement))) {
		    flag = true;
		    break;
		}
	    }

	    /* If the function contains some un-simplifyable compound or statement...: */
	    if (flag)
		return null;
	    /* do not simplify */

	}

	return null;
    }

    /**
     * <p>
     * Attempts to simplify the incoming Expression node. Depending on the type of the expression node and the result of descent into the
     * node provided, the Expression returned may be:
     * </p>
     *
     * <ul>
     * <li>The original Expression object un-modified, in the case the expression cannot be simplified (such as when containing varrefs,
     * funccalls, funcvarcalls, etc);</li>
     * <li>A mutated version of the Expression, in the case that only one of the left- or right-hand side(s) of the expression could be
     * simplified; or</li>
     * <li>A LiteralExpression constant value as a result of complete simplification of the expression component.</li>
     * </ul>
     *
     * <p>
     * In the case that an entire Expression is provided, the Expression returned shall be the most simplified version of that expression by
     * simplification of terms &amp; expansion of constants.
     * </p>
     *
     *
     *
     * @param originChunk The Chunk scope in which the Expression was defined
     * @param optFuncClosure (optional) The function closure in which the Expression was defined
     * @param optClosureStack (optional) a stack-ordered (top to bottom) List collection of Closure(s) which shall be checked (in stack
     * order) when looking for locals.
     * @param node The expression object to resolve, top-level node
     *
     * @return Depending on the nature of the Expression provided, the original Expression un-modified or a mutated version of the
     * Expression modified or a LiteralValue indicating total simplification of the expression provided.
     */
    public static Expression simplifyExpressionTree(Chunk originChunk, FunctionDecl optFuncClosure, List<Closure> optClosureStack,
	    Expression node) throws Exception {

	if (node instanceof LiteralValue)
	    /* doesn't get any simpler already */
	    return node;

	if (node instanceof FunctionRefExpression)
	    /* cannot simplify a pointer value lol */
	    return node;

	/*
	* cannot simplify function descent without first doing function_simplify >:-)
	 */
	if (node instanceof VariableRefExpression | node instanceof FunctionCallExpression
		| node instanceof FunctionCallVarExpression)
	    return node;

	/**
	 * If the expression is a unary term...:
	 */
	if (node instanceof UnaryExpression) {
	    UnaryExpression unExpr = (UnaryExpression) node;

	    /* ... don't simplify what is already itself simple ... */
	    Expression simpleRhs = (unExpr.rhs instanceof LiteralValue) ? unExpr.rhs
		    : simplifyExpressionTree(originChunk, optFuncClosure, optClosureStack, unExpr.rhs);

	    LiteralValue mutatedLiteral = null;
	    Object aNumberTypeObj = null;

	    /* If simplification of the term in the unary results in a literal value...: */
	    if (simpleRhs instanceof LiteralValue) {
		LiteralValue computedLiteral = (LiteralValue) simpleRhs;
		/*  All term(s) required are resolved, so now execute the operand outside the unary...: */
		switch (unExpr.operand) {
		    case BIT_NEGATE:
			if (computedLiteral.typeOfValue != TypeDecl.TYPE_INTEGER)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer for bitwise negation, got %s (at %s)",
				    computedLiteral.typeOfValue, unExpr.location));
			/* .... returning a new literal in place of this unary... */
			mutatedLiteral = new LiteralValue();
			/* ... whose location is the operand token location... */
			mutatedLiteral.location = unExpr.location;
			/* .. and whose type is the resulttype of the unary */
			mutatedLiteral.typeOfValue = computedLiteral.typeOfValue;
			/* and whose value is the result of the unary, applied to the literal: */
			mutatedLiteral.valueObject = ~((Integer) computedLiteral.valueObject);
			/* return the literal value of the entire unary expr */
			return mutatedLiteral;
		    case BOOL_NOT:
			if (computedLiteral.typeOfValue != TypeDecl.TYPE_BOOLEAN)
			    throw new Exception(String.format("Expected boolean for boolean NOT, got %s (at %s)",
				    computedLiteral.typeOfValue, unExpr.location));
			mutatedLiteral = new LiteralValue();
			mutatedLiteral.location = unExpr.location;
			mutatedLiteral.typeOfValue = computedLiteral.typeOfValue;
			mutatedLiteral.valueObject = !((Boolean) computedLiteral.valueObject);
			return mutatedLiteral;
		    case NUM_ABSVAL:
			if (computedLiteral.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedLiteral.typeOfValue != TypeDecl.TYPE_REAL)
			    throw new Exception(String.format("Expected integer or real for ABSVAL, got %s (at %s)",
				    computedLiteral.typeOfValue, unExpr.location));
			mutatedLiteral = new LiteralValue();
			mutatedLiteral.location = unExpr.location;
			mutatedLiteral.typeOfValue = computedLiteral.typeOfValue;
			/* type shall remain consistent */

			aNumberTypeObj = computedLiteral.typeOfValue;
			if (aNumberTypeObj instanceof Integer)
			    /* type int ?? :( */
			    mutatedLiteral.valueObject = Math.abs((Integer) aNumberTypeObj);
			else if (aNumberTypeObj instanceof Double)
			    mutatedLiteral.valueObject = Math.abs((Double) aNumberTypeObj);
			else if (aNumberTypeObj instanceof Float)
			    mutatedLiteral.valueObject = Math.abs((Float) aNumberTypeObj);
			else
			    throw new Exception(String.format("Unsupported internal LiteralValue value object class %s",
				    aNumberTypeObj.getClass().getName()));
			return mutatedLiteral;
		    case NUM_NEGATE:
			if (computedLiteral.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedLiteral.typeOfValue != TypeDecl.TYPE_REAL)
			    throw new Exception(String.format("Expected integer or real for ABSVAL, got %s (at %s)",
				    computedLiteral.typeOfValue, unExpr.location));
			mutatedLiteral = new LiteralValue();
			mutatedLiteral.location = unExpr.location;
			mutatedLiteral.typeOfValue = computedLiteral.typeOfValue;
			/* type shall remain consistent */

			aNumberTypeObj = computedLiteral.typeOfValue;
			if (aNumberTypeObj instanceof Integer)
			    /* type int ?? :( */
			    mutatedLiteral.valueObject = -((Integer) aNumberTypeObj);
			else if (aNumberTypeObj instanceof Double)
			    mutatedLiteral.valueObject = -((Double) aNumberTypeObj);
			else if (aNumberTypeObj instanceof Float)
			    mutatedLiteral.valueObject = -((Float) aNumberTypeObj);
			else
			    throw new Exception(String.format("Unsupported internal LiteralValue value object class %s",
				    aNumberTypeObj.getClass().getName()));
			return mutatedLiteral;
		    default:
			throw new Exception(String.format(
				"Expected simplification for LiteralValue right-side result, unsupported operand %s",
				unExpr.operand));
		}

	    } else if (simpleRhs != unExpr.rhs) {
		/* ... not simplified, the expr changed: return a copyobj of this unary... */
		UnaryExpression ret = new UnaryExpression();
		/* ... with the same location... */
		ret.location = unExpr.location;
		/* ... with the same operand ... */
		ret.operand = unExpr.operand;
		/* and with the simplified-expression */
		ret.rhs = simpleRhs;
		/* return the simplified unary */
		return ret;
	    } else
		/* the expression was not simplified at all, return the original: */
		return unExpr;

	} else if (node instanceof BinaryExpression) {
	    /*
	     * else if the expression is a binary term...:
	     */

	    BinaryExpression binExpr = (BinaryExpression) node;

	    /* .. don't simplify what is already itself simple... */
	    Expression simpleLhs = (binExpr.lhs instanceof LiteralValue) ? binExpr.lhs
		    : simplifyExpressionTree(originChunk, optFuncClosure, optClosureStack, binExpr.lhs),
		    simpleRhs = (binExpr.rhs instanceof LiteralValue) ? binExpr.rhs
			    : simplifyExpressionTree(originChunk, optFuncClosure, optClosureStack, binExpr.rhs);

	    /**
	     * if left and right-hand-side expressions both simplify to literals...:
	     */
	    if (simpleLhs instanceof LiteralValue
		    && simpleRhs instanceof LiteralValue) {
		/*
		 * simplification(lhs, rhs) -> literalvalue, literalvalue ?
		 */
		LiteralValue computedLhs = (LiteralValue) simpleLhs, computedRhs = (LiteralValue) simpleRhs;
		LiteralValue mutatedLiteral = null;
		Object lhsValObj = computedLhs.valueObject, rhsValObj = computedRhs.valueObject;

		/* first, some type checking... */
		switch (binExpr.operand) {
		    case BIT_AND:
		    case BIT_OR:
		    case BIT_SHL:
		    case BIT_SHR:
		    case BIT_XOR:
			/* both sides must be INTEGER: */
			if (computedLhs.typeOfValue != TypeDecl.TYPE_INTEGER)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer for bitwise %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedLhs.typeOfValue, binExpr.lhs.location));

			if (computedRhs.typeOfValue != TypeDecl.TYPE_INTEGER)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer for bitwise %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedRhs.typeOfValue, binExpr.rhs.location));
			break;

		    case BOOL_AND:
		    case BOOL_OR:
			/* both sides must be BOOLEAN: */
			if (computedLhs.typeOfValue != TypeDecl.TYPE_BOOLEAN)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected boolean for boolean %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedLhs.typeOfValue, binExpr.lhs.location));

			if (computedRhs.typeOfValue != TypeDecl.TYPE_BOOLEAN)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected boolean for boolean %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedRhs.typeOfValue, binExpr.rhs.location));

			break;

		    case NUM_ADD:
		    case NUM_DIV:
		    case NUM_MUL:
		    case NUM_SUB:
			/* both sides must be INTEGER or REAL: */
			if (computedLhs.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedLhs.typeOfValue != TypeDecl.TYPE_REAL
				&& computedLhs.typeOfValue != TypeDecl.TYPE_LONG)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedLhs.typeOfValue, binExpr.lhs.location));

			if (computedRhs.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedRhs.typeOfValue != TypeDecl.TYPE_REAL
				&& computedRhs.typeOfValue != TypeDecl.TYPE_LONG)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedRhs.typeOfValue, binExpr.rhs.location));
			break;

		    case NUM_POW:
		    case NUM_MOD:
			/*
			 * the left-hand-side may be INTEGER or REAL, but the right-hand-side must be INTEGER:
			 */
			if (computedLhs.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedLhs.typeOfValue != TypeDecl.TYPE_REAL
				&& computedRhs.typeOfValue != TypeDecl.TYPE_LONG)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedLhs.typeOfValue, binExpr.lhs.location));

			if (computedRhs.typeOfValue != TypeDecl.TYPE_INTEGER)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedRhs.typeOfValue, binExpr.rhs.location));
			break;

		    case VAL_EQ:
		    case VAL_NEQ:
			/* the left- and right-hand side may be ANY type: */
			break;

		    case VAL_LT:
		    case VAL_GT:
		    case VAL_GTEQ:
		    case VAL_LTEQ:
			/* The left- and right-hand-side may be INTEGER or REAL */
			if (computedLhs.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedLhs.typeOfValue != TypeDecl.TYPE_REAL
				&& computedRhs.typeOfValue != TypeDecl.TYPE_LONG)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedLhs.typeOfValue, binExpr.lhs.location));

			if (computedRhs.typeOfValue != TypeDecl.TYPE_INTEGER
				&& computedRhs.typeOfValue != TypeDecl.TYPE_REAL
				&& computedRhs.typeOfValue != TypeDecl.TYPE_LONG)
			    /* expression would have resulted in illegal production anyway ? */
			    throw new Exception(String.format("Expected integer or real for numerical %s, got %s (at %s)",
				    binExpr.operand.name().substring(3), computedRhs.typeOfValue, binExpr.rhs.location));
			break;

		    default:
			throw new Exception(String.format(
				"Expected simplification result for LiteralValue left-right binary expression, unsupported %s (at %s)",
				binExpr.operand, binExpr.location));
		}

		/* prepare a new literal in place of the entire binary expression...: */
		mutatedLiteral = new LiteralValue();
		/* ... who shall share the location of the original expression... */
		mutatedLiteral.location = binExpr.location;

		/* execute the operand on the left- and right-hand-side literal solutions...: */
		switch (binExpr.operand) {

		    case BIT_AND:
		    case BIT_OR:
		    case BIT_SHL:
		    case BIT_SHR:
		    case BIT_XOR: {
			if (!(lhsValObj instanceof Integer))
			    throw new Exception(
				    String.format("Unsupported internal LiteralValue (class %s) for binary bitop (at %s)",
					    lhsValObj.getClass().getName(), binExpr.lhs));
			if (!(rhsValObj instanceof Integer))
			    throw new Exception(
				    String.format("Unsupported internal LiteralValue (class %s) for binary bitop (at %s)",
					    rhsValObj.getClass().getName(), binExpr.rhs));

			Integer lhsInt = (Integer) lhsValObj, rhsInt = (Integer) rhsValObj;

			switch (binExpr.operand) {
			    case BIT_AND:
				mutatedLiteral.valueObject = lhsInt & rhsInt;
				break;
			    case BIT_OR:
				mutatedLiteral.valueObject = lhsInt | rhsInt;
				break;
			    case BIT_SHL:
				mutatedLiteral.valueObject = lhsInt << rhsInt;
				break;
			    case BIT_SHR:
				mutatedLiteral.valueObject = lhsInt >> rhsInt;
				break;
			    default:
				throw new Exception("illegal state");

			}

			mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
			break;
		    }

		    case BOOL_AND:
		    case BOOL_OR: {
			if (!(lhsValObj instanceof Boolean))
			    throw new Exception(
				    String.format("Unsupported internal LiteralValue (class %s) for binary boolop (at %s)",
					    lhsValObj.getClass().getName(), binExpr.lhs));
			if (!(rhsValObj instanceof Boolean))
			    throw new Exception(
				    String.format("Unsupported internal LiteralValue (class %s) for binary boolop (at %s)",
					    rhsValObj.getClass().getName(), binExpr.rhs));

			Boolean lhsBool = (Boolean) lhsValObj, rhsBool = (Boolean) rhsValObj;

			switch (binExpr.operand) {
			    case BOOL_AND:
				mutatedLiteral.valueObject = lhsBool && rhsBool;
				break;
			    case BOOL_OR:
				mutatedLiteral.valueObject = lhsBool || rhsBool;
				break;
			    default:
				throw new Exception("Illegal state");
			}

			mutatedLiteral.typeOfValue = TypeDecl.TYPE_BOOLEAN;
			break;
		    }
		    case NUM_ADD:
		    case NUM_DIV:
		    case NUM_MUL:
		    case NUM_SUB: {
			/* all other numerical operations are purely numerical: */

			Double d0 = null, d1 = null;
			Integer i0 = null, i1 = null;
			Long l0 = null, l1 = null;

			if (lhsValObj instanceof Double)
			    d0 = (Double) lhsValObj;
			else if (lhsValObj instanceof Float)
			    d0 = ((Float) lhsValObj).doubleValue();
			else if (lhsValObj instanceof Integer)
			    i0 = (Integer) lhsValObj;
			else if (lhsValObj instanceof Long)
			    l0 = (Long) lhsValObj;

			if (rhsValObj instanceof Double)
			    d1 = (Double) rhsValObj;
			else if (rhsValObj instanceof Float)
			    d1 = ((Float) rhsValObj).doubleValue();
			else if (rhsValObj instanceof Integer)
			    i1 = (Integer) rhsValObj;
			else if (rhsValObj instanceof Long)
			    l1 = (Long) rhsValObj;

			switch (binExpr.operand) {

			    case NUM_ADD:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 + l1;
				    else if (l0 != null)
					if (d1 != null)
					    mutatedLiteral.valueObject = l0 + d1.longValue();
					else
					    mutatedLiteral.valueObject = l0 + i1.longValue();
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0.doubleValue() + l1;
				    else
					mutatedLiteral.valueObject = i0.doubleValue() + l1;
				else if (d0 != null || d1 != null) {
				    if (d0 != null && d1 != null)
					mutatedLiteral.valueObject = d0 + d1;
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0 + i1.doubleValue();
				    else
					mutatedLiteral.valueObject = i0.doubleValue() + d1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				} else {
				    /* ADD (int, int) -> int */
				    mutatedLiteral.valueObject = i0 + i1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				}
				break;
			    case NUM_SUB:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 - l1;
				    else if (l0 != null)
					if (d1 != null)
					    mutatedLiteral.valueObject = l0 - d1.longValue();
					else
					    mutatedLiteral.valueObject = l0 - i1.longValue();
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0.doubleValue() - l1;
				    else
					mutatedLiteral.valueObject = i0.doubleValue() - l1;
				else if (d0 != null || d1 != null) {
				    /* SUB (double, any | any, double) -> double */
				    if (d0 != null && d1 != null)
					/* -> SUB ( double, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = d0 + d1;
				    else if (d0 != null)
					/* -> SUB ( double, int ) -> double (as REAL) */
					mutatedLiteral.valueObject = d0 - i1.doubleValue();
				    else
					/* -> SUB ( int, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = i0.doubleValue() - d1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				} else {
				    /* SUB (int, int) -> int */
				    mutatedLiteral.valueObject = i0 - i1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				}
				break;
			    case NUM_DIV:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 / l1;
				    else if (l0 != null)
					if (d1 != null)
					    mutatedLiteral.valueObject = l0 / d1.longValue();
					else
					    mutatedLiteral.valueObject = l0 / i1.longValue();
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0.doubleValue() / l1;
				    else
					mutatedLiteral.valueObject = i0.doubleValue() / l1;
				else if (d0 != null || d1 != null)
				    /* DIV (....) */
				    if (d0 != null && d1 != null) {
					/* -> DIV ( double, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = d0 / d1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				    } else if (d0 != null) {
					/* -> DIV ( double, int ) -> int (as INTEGER) */
					mutatedLiteral.valueObject = d0 / i1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				    } else {
					/* -> SUB ( int, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = i0.doubleValue() / d1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				    }
				else {
				    /* DIV (int, int) -> int */
				    mutatedLiteral.valueObject = i0 / i1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				}
				break;
			    case NUM_MUL:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 * l1;
				    else if (l0 != null)
					if (d1 != null)
					    mutatedLiteral.valueObject = l0 * d1.longValue();
					else
					    mutatedLiteral.valueObject = l0 * i1.longValue();
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0.doubleValue() * l1;
				    else
					mutatedLiteral.valueObject = i0.doubleValue() * l1;
				else if (d0 != null || d1 != null)
				    /* MUL (....) */
				    if (d0 != null && d1 != null) {
					/* -> MUL ( double, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = d0 * d1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				    } else if (d0 != null) {
					/* -> MUL ( double, int ) -> int (as INTEGER) */
					mutatedLiteral.valueObject = d0 * i1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				    } else {
					/* -> MUL ( int, double ) -> double (as REAL) */
					mutatedLiteral.valueObject = i0.doubleValue() * d1;
					mutatedLiteral.typeOfValue = TypeDecl.TYPE_REAL;
				    }
				else {
				    /* MUL (int, int) -> int */
				    mutatedLiteral.valueObject = i0 * i1;
				    mutatedLiteral.typeOfValue = TypeDecl.TYPE_INTEGER;
				}
				break;
			    default:
				throw new Exception("illegal state");
			}
			break;
		    }
		    case NUM_MOD:
		    case NUM_POW: {
			Double d0 = null;
			Integer i0 = null, i1 = null;
			Long l0 = null;

			if (lhsValObj instanceof Double)
			    d0 = (Double) lhsValObj;
			else if (lhsValObj instanceof Float)
			    d0 = ((Float) lhsValObj).doubleValue();
			else if (lhsValObj instanceof Integer)
			    i0 = (Integer) lhsValObj;
			else if (lhsValObj instanceof Long)
			    l0 = (Long) lhsValObj;

			if (!(rhsValObj instanceof Integer))
			    throw new Exception(String.format(
				    "Unsupported internal LiteralValue (class %s) for binary numerical int-rhs op (at %s)",
				    rhsValObj.getClass().getName(), binExpr.rhs));
			i1 = (Integer) rhsValObj;

			switch (binExpr.operand) {
			    case NUM_MOD: {
				if (l0 != null)
				    mutatedLiteral.valueObject = l0 % i1;
				else if (d0 != null)
				    mutatedLiteral.valueObject = d0 % i1;
				else
				    mutatedLiteral.valueObject = i0 % i1;
				break;
			    }
			    case NUM_POW: {
				if (l0 != null)
				    mutatedLiteral.valueObject = (Long) ((long) Math.pow((double) l0, (double) i1));
				else if (d0 != null)
				    mutatedLiteral.valueObject = Math.pow(d0, (double) i1);
				else
				    mutatedLiteral.valueObject = (Integer) ((int) Math.pow((double) i0, (double) i1));
				break;
			    }

			    default:
				throw new Exception("illegal state");
			}

			break;
		    }

		    case VAL_EQ:
		    case VAL_NEQ: {
			mutatedLiteral.typeOfValue = TypeDecl.TYPE_BOOLEAN;
			if (computedLhs.typeOfValue == TypeDecl.TYPE_STRING || computedLhs.typeOfValue == TypeDecl.TYPE_STRING) {
			    String s0 = (lhsValObj instanceof String) ? (String) lhsValObj : lhsValObj.toString();
			    String s1 = (rhsValObj instanceof String) ? (String) rhsValObj : rhsValObj.toString();
			    switch (binExpr.operand) {
				case VAL_EQ:
				    mutatedLiteral.valueObject = s0.equals(s1);
				    break;
				case VAL_NEQ:
				    mutatedLiteral.valueObject = !s0.equals(s1);
				    break;
				default:
				    throw new Exception("illegal state");
			    }
			} else {
			    Double d0 = null, d1 = null;
			    Integer i0 = null, i1 = null;
			    Boolean b0 = null, b1 = null;
			    Long l0 = null, l1 = null;

			    if (lhsValObj instanceof Double)
				d0 = (Double) lhsValObj;
			    if (lhsValObj instanceof Float)
				d0 = ((Float) lhsValObj).doubleValue();
			    if (lhsValObj instanceof Integer)
				i0 = (Integer) lhsValObj;
			    if (lhsValObj instanceof Boolean)
				b0 = (Boolean) lhsValObj;
			    if (lhsValObj instanceof Long)
				l0 = (Long) lhsValObj;

			    if (rhsValObj instanceof Double)
				d1 = (Double) rhsValObj;
			    if (rhsValObj instanceof Float)
				d1 = ((Float) rhsValObj).doubleValue();
			    if (rhsValObj instanceof Integer)
				i1 = (Integer) rhsValObj;
			    if (rhsValObj instanceof Boolean)
				b1 = (Boolean) rhsValObj;
			    if (rhsValObj instanceof Long)
				l1 = (Long) rhsValObj;

			    if (b0 != null || b1 != null) {
				if (b0 == null || b1 == null)
				    throw new Exception(String.format("Expected boolean, boolean for operand %s, got %s, %s",
					    binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));

				switch (binExpr.operand) {
				    case VAL_EQ:
					mutatedLiteral.valueObject = (b0 && b1);
					break;
				    case VAL_NEQ:
					mutatedLiteral.valueObject = (b0 && !b1) || (!b0 && b1);
					break;
				    default:
					throw new Exception("illegal state");
				}
			    } else if (l0 != null || l1 != null)
				if (l0 != null && l1 != null)
				    switch (binExpr.operand) {
					case VAL_EQ:
					    mutatedLiteral.valueObject = (((long) l0) == ((long) l1));
					    break;
					case VAL_NEQ:
					    mutatedLiteral.valueObject = (((long) l0) != ((long) l1));
					    break;
					default:
					    throw new Exception("illegal state");
				    }
				else
				    throw new Exception(String.format("Expected long, long for operand %s where one argument is long, got %s, %s",
					    binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));
			    else if (d0 != null || d1 != null)
				if (d0 != null && d1 != null)
				    switch (binExpr.operand) {
					case VAL_EQ:
					    mutatedLiteral.valueObject = (((double) d0) == ((double) d1));
					    break;
					case VAL_NEQ:
					    mutatedLiteral.valueObject = (((double) d0) != ((double) d1));
					    break;
					default:
					    throw new Exception("illegal state");
				    }
				else if (d0 != null)
				    switch (binExpr.operand) {
					case VAL_EQ:
					    mutatedLiteral.valueObject = (((double) d0) == ((int) i1));
					    break;
					case VAL_NEQ:
					    mutatedLiteral.valueObject = (((double) d0) != ((int) i1));
					    break;
					default:
					    throw new Exception("illegal state");
				    }
				else
				    switch (binExpr.operand) {
					case VAL_EQ:
					    mutatedLiteral.valueObject = (((int) i0) == ((double) d1));
					    break;
					case VAL_NEQ:
					    mutatedLiteral.valueObject = (((int) i0) != ((double) d1));
					    break;
					default:
					    throw new Exception("illegal state");
				    }
			    else
				switch (binExpr.operand) {
				    case VAL_EQ:
					mutatedLiteral.valueObject = (((int) i0) == ((int) i1));
					break;
				    case VAL_NEQ:
					mutatedLiteral.valueObject = (((int) i0) != ((int) i1));
					break;
				    default:
					throw new Exception("illegal state");
				}
			}
			break;
		    }

		    case VAL_GT:
		    case VAL_LT:
		    case VAL_GTEQ:
		    case VAL_LTEQ: {
			mutatedLiteral.typeOfValue = TypeDecl.TYPE_BOOLEAN;

			Double d0 = null, d1 = null;
			Integer i0 = null, i1 = null;
			Long l0 = null, l1 = null;

			if (lhsValObj instanceof Double)
			    d0 = (Double) lhsValObj;
			else if (lhsValObj instanceof Float)
			    d0 = ((Float) lhsValObj).doubleValue();
			else if (lhsValObj instanceof Integer)
			    i0 = (Integer) lhsValObj;
			else if (lhsValObj instanceof Long)
			    l0 = (Long) lhsValObj;

			if (rhsValObj instanceof Double)
			    d1 = (Double) rhsValObj;
			else if (rhsValObj instanceof Float)
			    d1 = ((Float) rhsValObj).doubleValue();
			else if (rhsValObj instanceof Integer)
			    i1 = (Integer) rhsValObj;
			else if (lhsValObj instanceof Long)
			    l1 = (Long) rhsValObj;

			switch (binExpr.operand) {
			    case VAL_GT:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 > l1;
				    else
					throw new Exception(String.format("Expected long, long for operand %s where one argument is long, got %s, %s",
						binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));
				else if (d0 != null || d1 != null)
				    if (d0 != null && d1 != null)
					mutatedLiteral.valueObject = d0 > d1;
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0 > i1;
				    else
					mutatedLiteral.valueObject = i0 > d1;
				else
				    mutatedLiteral.valueObject = i0 > i1;
				break;
			    case VAL_LT:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 < l1;
				    else
					throw new Exception(String.format("Expected long, long for operand %s where one argument is long, got %s, %s",
						binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));
				else if (d0 != null || d1 != null)
				    if (d0 != null && d1 != null)
					mutatedLiteral.valueObject = d0 < d1;
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0 < i1;
				    else
					mutatedLiteral.valueObject = i0 < d1;
				else
				    mutatedLiteral.valueObject = i0 < i1;
				break;
			    case VAL_GTEQ:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 >= l1;
				    else
					throw new Exception(String.format("Expected long, long for operand %s where one argument is long, got %s, %s",
						binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));
				else if (d0 != null || d1 != null)
				    if (d0 != null && d1 != null)
					mutatedLiteral.valueObject = d0 >= d1;
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0 >= i1;
				    else
					mutatedLiteral.valueObject = i0 >= d1;
				else
				    mutatedLiteral.valueObject = i0 >= i1;
				break;
			    case VAL_LTEQ:
				if (l0 != null || l1 != null)
				    if (l0 != null && l1 != null)
					mutatedLiteral.valueObject = l0 <= l1;
				    else
					throw new Exception(String.format("Expected long, long for operand %s where one argument is long, got %s, %s",
						binExpr.operand, computedLhs.typeOfValue, computedRhs.typeOfValue));
				else if (d0 != null || d1 != null)
				    if (d0 != null && d1 != null)
					mutatedLiteral.valueObject = d0 <= d1;
				    else if (d0 != null)
					mutatedLiteral.valueObject = d0 <= i1;
				    else
					mutatedLiteral.valueObject = i0 <= d1;
				else
				    mutatedLiteral.valueObject = i0 <= i1;
				break;
			    default:
				throw new Exception("illegal state");
			}
			break;
		    }
		    default:
			throw new Exception("illegal state");
		}

		/**
		 * assert that an operand was selected and that it set the type:
		 */
		if (mutatedLiteral.typeOfValue == null)
		    throw new Exception(
			    String.format("Detected unsupported simplification binary operand %s", binExpr.operand));

		/**
		 * return the simplified literal solution of the binary expression:
		 */
		return mutatedLiteral;
	    } else if (simpleLhs != binExpr.lhs || simpleRhs != binExpr.rhs) {
		/* else one of the left or right-hand side expression(s) was simplified, so....: */
		BinaryExpression ret = new BinaryExpression();
		/* ... inherit the original expression location... */
		ret.location = binExpr.location;
		/* ... with the same binary operand... */
		ret.operand = binExpr.operand;
		if (simpleLhs != null)
		    /* the lhs was simplified: */
		    ret.lhs = simpleLhs;
		else
		    /* use the original lhs expr */
		    ret.lhs = binExpr.lhs;
		if (simpleRhs != null)
		    /* the rhs was simplified: */
		    ret.rhs = simpleRhs;
		else
		    /* use the original rhs expr */
		    ret.rhs = binExpr.rhs;
		/* return the simplified binaryexpr, who may have one or both term(s) simplified: */
		return ret;
	    } else
		/* no simplification at all; return the original expr */
		return binExpr;

	} else if (node instanceof ParenExpression) {
	    /* the node is a parenthethis expression...: */
	    ParenExpression parenthExpression = (ParenExpression) node;
	    Expression simpleRhs = (parenthExpression.expressionInParenth instanceof LiteralValue)
		    ? parenthExpression.expressionInParenth
		    : simplifyExpressionTree(originChunk, optFuncClosure, optClosureStack, parenthExpression.expressionInParenth);
	    if (simpleRhs instanceof LiteralValue)
		/* if the value is a literal */
		return simpleRhs;
	    /* the literal can replace the entire parenthesis */

	    if (simpleRhs != parenthExpression.expressionInParenth) {
		/* internal expression was simplified, return new parenth: */
		ParenExpression ret = new ParenExpression();
		/* ... with the same location as the original... */
		ret.location = parenthExpression.location;
		/* ... with the simplified internal expression: */
		ret.expressionInParenth = simpleRhs;
		/* return simplification of internal term(s): */
		return ret;
	    } else
		/* no simplification at all; return the original parenexpr */
		return parenthExpression;
	} else
	    /* the node isn't in the IFSWITCH, so complain: */
	    throw new Exception(String.format(
		    "Unsupported Expression type node (class %s); a declaration for the type "
		    + "must be included in simplifyExpressionTree (defined by token in user-source at %s)",
		    node.getClass().getName(), node.location));
    }

    /**
     * <p>
     * Compute the type of the expression using descent into the node provided. All leaves of the node shall be checked to ensure the
     * expression resolves to some type. The return TypeDecl indicates the expected result type of the expression, which shall be one of:
     * </p>
     *
     * <ul>
     * <li>A TypeDecl base type of some form (TYPE_BOOLEAN, TYPE_INTEGER, TYPE_REAL, TYPE_STRING);</li>
     * <li>A TypeDecl base type indicating a function pointer (TYPE_CODE);</li>
     * <li>A TypeDecl base type of <code>TYPE_NOTHING</code>, indicating that the expression is unsupported; or</li>
     * <li><code>null</code> indicating that the type of the expression cannot be resolved due to <code>callfuncvar</code> expression
     * parts.</li>
     * </ul>
     *
     * <p>
     * Where the expression contains binary operands dealing with numbers, the following pattern shall apply:
     * </p>
     *
     * <ul>
     * <li>Where one or more sides of the operand is a <code>real</code>-type number, the product of the expression shall be
     * <code>real</code>;</li>
     * <li>Where both sides of the operand are a <code>integer</code>-type number, the product of the expression shall be
     * <code>integer</code>, truncated.</li>
     * </ul>
     *
     * <p>
     * Where the expression is a unary operand, the type of the right-hand side shall be checked to confirm it is a <code>boolean</code>-,
     * an <code>integer</code>- or a <code>real</code>-type, depending on the type of the unary operand in use.
     * </p>
     *
     * <p>
     * Where the expression is a variable reference, the type of the variable shall be checked following closure-order:
     * </p>
     *
     * <ul>
     * <li>The stack closure (for);</li>
     * <li>The local closure (function closure locals);</li>
     * <li>The global closure (globalvar);</li>
     * <li>Imported variable symbols;</li>
     * </ul>
     *
     * <p>
     * Where the expression is a function call expression, the return-type of the function shall be checked in the following closure-order:
     * </p>
     *
     * <ul>
     * <li>The global closure, native functions (nfuncs);</li>
     * <li>The global closure, user functions (ufuncs);</li>
     * <li>Imported function symbols;</li>
     * </ul>
     *
     * @param originChunk The Chunk scope in which the Expression was defined
     * @param optFuncClosure (optional) The function closure in which the Expression was defined
     * @param optClosureStack (optional) a stack-ordered (top to bottom) List collection of Closure(s) which shall be checked (in stack
     * order) when looking for locals.
     * @param node The expression object to resolve, top-level node
     *
     * @return A TypeDecl indicating the type (BOOLEAN, INTEGER, REAL, STRING, CODE, HANDLE) or NOTHING if invalid or <code>null</code> if
     * the expression cannot be resolved from the current level
     *
     * @throws TypeMismatchException for unary expressions, if the expression right-hand side does not resolve to a type applicable for the
     * unary expression operand a TypeMismatchException shall be thrown; for binary expressions, if the expression left&amp;right hand sides
     * type(s) do not resolve to applicable and/or compatible types a TypeMismatchException shall be thrown
     * @throws Exception if an unresolvable symbol is encountered, an Exception shall be thrown indicating the type of symbol (function,
     * variable) &amp; the token location of the missing symbol
     */
    public static TypeDecl computeExpressionResultType(Chunk originChunk, FunctionDecl optFuncClosure, List<Closure> optClosureStack,
	    Expression node)
	    throws TypeMismatchException, Exception {

	/* recurse into ParenExpression for interior type: */
	if (node instanceof ParenExpression)
	    return computeExpressionResultType(originChunk, optFuncClosure, optClosureStack,
		    ((ParenExpression) node).expressionInParenth);

	/* cases for UNARY expressions: */
	else if (node instanceof UnaryExpression) {
	    /* compute the type of the right-hand-side: */
	    TypeDecl computedRhsType = computeExpressionResultType(originChunk, optFuncClosure, optClosureStack,
		    ((UnaryExpression) node).rhs);

	    /* type-checking:: */
	    switch (((UnaryExpression) node).operand) {
		case BOOL_NOT:
		    /* UNARY_BOOLEAN_NOT ( boolean ) */
		    if (computedRhsType != TypeDecl.TYPE_BOOLEAN)
			throw new TypeMismatchException(TypeDecl.TYPE_BOOLEAN, computedRhsType, node);
		    break;
		case BIT_NEGATE:
		    /* UNARY_BIT_NEGATE ( int ) */
		    if (computedRhsType != TypeDecl.TYPE_INTEGER)
			throw new TypeMismatchException(TypeDecl.TYPE_INTEGER, computedRhsType, node);
		    break;
		case NUM_ABSVAL:
		/* UNARY_NUM_ABSVAL ( int | real ) */
		case NUM_NEGATE:
		    /* UNARY_NUM_NEGATE ( int | real ) */
		    if (computedRhsType != TypeDecl.TYPE_INTEGER && computedRhsType != TypeDecl.TYPE_REAL)
			throw new TypeMismatchException(TypeDecl.TYPE_REAL, computedRhsType,
				node);
		    /* show `widest` type allowed :( */
		    break;
	    }
	    /* return checked type: */
	    return computedRhsType;
	} else if (node instanceof BinaryExpression) {
	    /* cases for BINARY expressions: compute both sides types: */
	    TypeDecl computedLhsType = computeExpressionResultType(originChunk, optFuncClosure, optClosureStack,
		    ((BinaryExpression) node).lhs),
		    computedRhsType = computeExpressionResultType(originChunk, optFuncClosure, optClosureStack,
			    ((BinaryExpression) node).rhs);

	    /* type-checking:: */
	    switch (((BinaryExpression) node).operand) {
		case BIT_AND:
		case BIT_OR:
		case BIT_SHL:
		case BIT_SHR:
		case BIT_XOR:
		    if (computedLhsType != TypeDecl.TYPE_INTEGER)
			throw new TypeMismatchException(TypeDecl.TYPE_INTEGER, computedLhsType,
				((BinaryExpression) node).lhs);
		    if (computedRhsType != TypeDecl.TYPE_INTEGER)
			throw new TypeMismatchException(TypeDecl.TYPE_INTEGER, computedRhsType,
				((BinaryExpression) node).rhs);
		    /* BINARY_BIT_* ( int, int ) */
		    return TypeDecl.TYPE_INTEGER;
		case BOOL_AND:
		case BOOL_OR:
		    if (computedLhsType != TypeDecl.TYPE_BOOLEAN)
			throw new TypeMismatchException(TypeDecl.TYPE_BOOLEAN, computedLhsType,
				((BinaryExpression) node).lhs);
		    if (computedRhsType != TypeDecl.TYPE_BOOLEAN)
			throw new TypeMismatchException(TypeDecl.TYPE_BOOLEAN, computedRhsType,
				((BinaryExpression) node).rhs);
		    /* BINARY_BOOL_* ( boolean, boolean ) */
		    return TypeDecl.TYPE_BOOLEAN;
		case NUM_ADD:
		case NUM_SUB:
		case NUM_DIV:
		case NUM_MUL:
		    /* operators on LONG type always return LONG numbers, regardless of other params: */
		    if (computedLhsType == TypeDecl.TYPE_LONG || computedRhsType == TypeDecl.TYPE_LONG)
			return TypeDecl.TYPE_LONG;
		    /* all other numerical operations require REAL or INTEGER numbers only: */
		    return (computedLhsType == TypeDecl.TYPE_REAL || computedRhsType == TypeDecl.TYPE_REAL)
			    ? /* BINARY_NUM_* ( [int|real], [int|real] ) */ TypeDecl.TYPE_REAL
			    : /* BINARY_NUM_* ( int, int ) */ TypeDecl.TYPE_INTEGER;
		case NUM_MOD:
		case NUM_POW:
		    if (computedRhsType != TypeDecl.TYPE_INTEGER)
			throw new TypeMismatchException(TypeDecl.TYPE_INTEGER, computedRhsType,
				((BinaryExpression) node).rhs);
		    /* BINARY_NUM_MOD, BINARY_NUM_POW ( real, int ) */
		    return computedLhsType;
		default:
		    /* illegal */
		    return TypeDecl.TYPE_NOTHING;
	    }
	} else if (node instanceof VariableRefExpression) {
	    VariableRefExpression varRef = (VariableRefExpression) node;

	    if (optClosureStack != null) {
		Iterator<Closure> itx = optClosureStack.iterator();
		while (itx.hasNext()) {
		    Closure closureInScope = itx.next();
		    if (closureInScope.hascvars()) {
			int j = closureInScope.numcvars();
			if (j > 0)
			    for (int i = 0; i < j; i++)
				if (closureInScope.getcvarname(i).equals(varRef.nameOfLocalOrGlobalVar))
				    return closureInScope.getcvartype(i);
		    }
		}
	    }

	    if (optFuncClosure != null) {
		if (optFuncClosure.lvars != null
			&& optFuncClosure.lvars.size() > 0) {
		    Iterator<FunctionLVarDecl> itx = optFuncClosure.lvars.iterator();
		    while (itx.hasNext()) {
			FunctionLVarDecl localVar = itx.next();
			if (localVar.nameOfLocalVar.equals(varRef.nameOfLocalOrGlobalVar))
			    return localVar.valueType;
		    }
		}
		
		if ( optFuncClosure.paramNames != null &&
			    optFuncClosure.paramNames.size() > 0 ) {
		    for ( int i = 0, j = optFuncClosure.paramNames.size(); i < j; i++ ) {
			if ( optFuncClosure.paramNames.get(i).equals(varRef.nameOfLocalOrGlobalVar)) {
			    return optFuncClosure.paramTypes.get(i);
			}
		    }
		}
	    }

	    if (originChunk.globalsDecls != null
		    && originChunk.globalsDecls.size() > 0) {
		Iterator<GlobalsBlock> itx = originChunk.globalsDecls.iterator();
		while (itx.hasNext()) {
		    GlobalsBlock blockOfGlobals = itx.next();
		    Iterator<GlobalsVarDecl> itx1 = blockOfGlobals.iterator();
		    while (itx1.hasNext()) {
			GlobalsVarDecl globalVar = itx1.next();
			if (globalVar.nameOfGlobalVar.equals(varRef.nameOfLocalOrGlobalVar))
			    return globalVar.valueType;
		    }
		}
	    }

	    throw new Exception(String.format("No such variable %s in scope (at %s)", varRef.nameOfLocalOrGlobalVar, varRef.location));
	} else if (node instanceof FunctionCallExpression) {
	    FunctionCallExpression funcRef = (FunctionCallExpression) node;

	    Iterator<FunctionDecl> itx = originChunk.nfuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl nfuncdec = itx.next();
		if (nfuncdec.name.equals(funcRef.nameOfFunction))
		    return nfuncdec.returnType;
	    }

	    itx = originChunk.ufuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl ufuncdec = itx.next();
		if (ufuncdec.name.equals(funcRef.nameOfFunction))
		    return ufuncdec.returnType;
	    }

	    /**
	     * TODO: importedSymbol lookup
	     */
	    throw new Exception(
		    String.format("No such function %s in scope (at %s)", funcRef.nameOfFunction, funcRef.location));
	} else if (node instanceof FunctionCallVarExpression) {
	    FunctionCallVarExpression varfuncRef = (FunctionCallVarExpression) node;

	    /**
	     * TODO: the type technically cannot be determined in advance due to the fact that the var may be a lparam var; in this case the
	     * specific pointer cannot be dereferenced in advance to determine exactly what `function <IDENTIFIER>` was used to create the
	     * `code` pointer.
	     *
	     * We can do (and should do) if we can specifically simplify the value (if it is an lvar only, not assigned by parameter);
	     * otherwise, the answer is 'INDETERMINATE'. The VM is responsible for checking the type on the stack.
	     */
	    /**
	     * FIXME: no logic, just dumb!
	     */
	    return null;
	    /**
	     * return INDETERMINATE
	     */
	} else if (node instanceof FunctionRefExpression) {
	    FunctionRefExpression funcRef = (FunctionRefExpression) node;
	    Iterator<FunctionDecl> itx = originChunk.nfuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl nfuncdec = itx.next();
		if (nfuncdec.name.equals(funcRef.nameOfFunction))
		    return TypeDecl.TYPE_CODE;
	    }

	    itx = originChunk.ufuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl ufuncdec = itx.next();
		if (ufuncdec.name.equals(funcRef.nameOfFunction))
		    return TypeDecl.TYPE_CODE;
	    }

	    /**
	     * TODO: importedSymbol lookup
	     */
	    throw new Exception(
		    String.format("No such function %s in scope (at %s)", funcRef.nameOfFunction, funcRef.location));

	} else if (node instanceof LiteralValue)
	    return ((LiteralValue) node).typeOfValue;
	else
	    return TypeDecl.TYPE_NOTHING;
	/* illegal */
    }

}
