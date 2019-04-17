package logicscript.assembler;

import logicbyte.bytecode.OpcodeParamValueTable.BinaryExpressionParamValueTable;
import logicbyte.bytecode.OpcodeParamValueTable.UnaryExpressionParamValueTable;
import logicscript.ast.nodes.expr.BinaryExpression.BinaryOperation;
import logicscript.ast.nodes.expr.UnaryExpression.UnaryOperation;

/**
 * <p>
 * Converts AST unary &amp; binary operands to bytecode oparand values, and visa-versa. This is externalized from other classes so that the
 * bytecode classes need not reference the AST classes and visa-versa.</p>
 *
 * @author Lochie
 */
public class ExpressionParamConverter {

    public static UnaryExpressionParamValueTable fromASTUnaryToBytecodeUnary(UnaryOperation astOperandVal) {
	switch (astOperandVal) {
	    case BIT_NEGATE:
		return UnaryExpressionParamValueTable.BIT_NEGATE;
	    case BOOL_NOT:
		return UnaryExpressionParamValueTable.BOOL_NOT;
	    case NUM_ABSVAL:
		return UnaryExpressionParamValueTable.NUM_ABSVAL;
	    case NUM_NEGATE:
		return UnaryExpressionParamValueTable.NUM_NEGATE;
	}
	return UnaryExpressionParamValueTable.ILLEGAL;
    }

    public static BinaryExpressionParamValueTable fromASTBinaryToBytecodeBinary(BinaryOperation astOperandVal) {
	switch (astOperandVal) {
	    case BIT_AND:
		return BinaryExpressionParamValueTable.BIT_AND;
	    case BIT_OR:
		return BinaryExpressionParamValueTable.BIT_OR;
	    case BIT_SHL:
		return BinaryExpressionParamValueTable.BIT_SHL;
	    case BIT_SHR:
		return BinaryExpressionParamValueTable.BIT_SHR;
	    case BIT_XOR:
		return BinaryExpressionParamValueTable.BIT_XOR;

	    case BOOL_AND:
		return BinaryExpressionParamValueTable.BOOL_AND;
	    case BOOL_OR:
		return BinaryExpressionParamValueTable.BOOL_OR;

	    case NUM_ADD:
		return BinaryExpressionParamValueTable.NUM_ADD;
	    case NUM_SUB:
		return BinaryExpressionParamValueTable.NUM_SUB;
	    case NUM_MUL:
		return BinaryExpressionParamValueTable.NUM_MUL;
	    case NUM_DIV:
		return BinaryExpressionParamValueTable.NUM_DIV;
	    case NUM_POW:
		return BinaryExpressionParamValueTable.NUM_POW;
	    case NUM_MOD:
		return BinaryExpressionParamValueTable.NUM_MOD;

	    case VAL_EQ:
		return BinaryExpressionParamValueTable.VAL_EQ;
	    case VAL_GT:
		return BinaryExpressionParamValueTable.VAL_GT;
	    case VAL_GTEQ:
		return BinaryExpressionParamValueTable.VAL_GTEQ;
	    case VAL_LT:
		return BinaryExpressionParamValueTable.VAL_LT;
	    case VAL_LTEQ:
		return BinaryExpressionParamValueTable.VAL_LTEQ;
	    case VAL_NEQ:
		return BinaryExpressionParamValueTable.VAL_NEQ;
	}
	return BinaryExpressionParamValueTable.ILLEGAL;
    }

    public static UnaryOperation fromBytecodeUnaryToASTUnary(UnaryExpressionParamValueTable bytecodeOpVal) {
	switch (bytecodeOpVal) {

	    case BIT_NEGATE:
		return UnaryOperation.BIT_NEGATE;
	    case BOOL_NOT:
		return UnaryOperation.BOOL_NOT;
	    case NUM_ABSVAL:
		return UnaryOperation.NUM_ABSVAL;
	    case NUM_NEGATE:
		return UnaryOperation.NUM_NEGATE;

	}
	return null;
    }

    public static BinaryOperation fromBytecodeBinaryToASTBinary(BinaryExpressionParamValueTable bytecodeOpVal) {
	switch (bytecodeOpVal) {
	    case BIT_AND:
		return BinaryOperation.BIT_AND;
	    case BIT_OR:
		return BinaryOperation.BIT_OR;
	    case BIT_SHL:
		return BinaryOperation.BIT_SHL;
	    case BIT_SHR:
		return BinaryOperation.BIT_SHR;
	    case BIT_XOR:
		return BinaryOperation.BIT_XOR;

	    case BOOL_AND:
		return BinaryOperation.BOOL_AND;
	    case BOOL_OR:
		return BinaryOperation.BOOL_OR;

	    case NUM_ADD:
		return BinaryOperation.NUM_ADD;
	    case NUM_SUB:
		return BinaryOperation.NUM_SUB;
	    case NUM_MUL:
		return BinaryOperation.NUM_MUL;
	    case NUM_DIV:
		return BinaryOperation.NUM_DIV;
	    case NUM_POW:
		return BinaryOperation.NUM_POW;
	    case NUM_MOD:
		return BinaryOperation.NUM_MOD;

	    case VAL_EQ:
		return BinaryOperation.VAL_EQ;
	    case VAL_GT:
		return BinaryOperation.VAL_GT;
	    case VAL_GTEQ:
		return BinaryOperation.VAL_GTEQ;
	    case VAL_LT:
		return BinaryOperation.VAL_LT;
	    case VAL_LTEQ:
		return BinaryOperation.VAL_LTEQ;
	    case VAL_NEQ:
		return BinaryOperation.VAL_NEQ;

	}
	return null;
    }

}
