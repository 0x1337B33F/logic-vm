package logicbyte.bytecode;

/**
 * Provides parameter value table lookups. Ordinal is specifically not used in this instance in the case that, should an IDE (or user)
 * re-order the enumeration declaration(s) they would inadvertently change the bytecode operand value numbers.
 *
 *
 * @author Lochie
 */
public interface OpcodeParamValueTable {

    public String name();

    public int ordinal();

    public int value();

    /**
     * Lookup table for the first parameter (UnaryOperand) of the <code>Opcodes.UNOP</code> opcode.
     */
    public static enum UnaryExpressionParamValueTable implements OpcodeParamValueTable {

	/**
	 * The op is illegal, it will consume the stack top value but return <code>nothing</code>
	 */
	ILLEGAL(0),
	/**
	 * Numerical negation
	 */
	NUM_NEGATE(1),
	/**
	 * Numerical absolute value
	 */
	NUM_ABSVAL(2),
	/**
	 * Bitwise negation
	 */
	BIT_NEGATE(3),
	/**
	 * Boolean negation
	 */
	BOOL_NOT(4);

	public static UnaryExpressionParamValueTable get(int word) {
	    UnaryExpressionParamValueTable[] values = UnaryExpressionParamValueTable.values();
	    for (UnaryExpressionParamValueTable value : values)
		if (value.value() == word)
		    return value;
	    return UnaryExpressionParamValueTable.ILLEGAL;
	}

	final int m_value;

	UnaryExpressionParamValueTable(int paramValue) {
	    this.m_value = paramValue;
	}

	@Override
	public int value() {
	    return this.m_value;
	}
    }

    /**
     * Lookup table for the first parameter (BinaryOperand) of the <code>Opcodes.BINOP</code> opcode.
     */
    public static enum BinaryExpressionParamValueTable implements OpcodeParamValueTable {
	ILLEGAL(0),
	BIT_AND(1),
	BIT_OR(2),
	BIT_XOR(3),
	BIT_SHL(4),
	BIT_SHR(5),
	BOOL_AND(6),
	BOOL_OR(7),
	NUM_ADD(8),
	NUM_SUB(9),
	NUM_MUL(10),
	NUM_DIV(11),
	NUM_MOD(12),
	NUM_POW(13),
	VAL_LT(14),
	VAL_GT(15),
	VAL_LTEQ(16),
	VAL_GTEQ(17),
	VAL_EQ(18),
	VAL_NEQ(19);

	public static BinaryExpressionParamValueTable get(int word) {
	    BinaryExpressionParamValueTable[] values = BinaryExpressionParamValueTable.values();
	    for (BinaryExpressionParamValueTable value : values)
		if (value.value() == word)
		    return value;
	    return BinaryExpressionParamValueTable.ILLEGAL;
	}

	final int m_value;

	BinaryExpressionParamValueTable(int paramValue) {
	    this.m_value = paramValue;
	}

	@Override
	public int value() {
	    return this.m_value;
	}

    }

}
