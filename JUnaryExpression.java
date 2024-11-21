package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * This abstract base class is the AST node for an unary expression.
 * A unary expression has a single operand.
 */

abstract class JUnaryExpression extends JExpression {

    /** The operator. */
    private String operator;

    /** The operand. */
    protected JExpression arg;

    /**
     * Constructs an AST node for an unary expression given its line number, the
     * unary operator, and the operand.
     * 
     * @param line
     *            line in which the unary expression occurs in the source file.
     * @param operator
     *            the unary operator.
     * @param arg
     *            the operand.
     */

    protected JUnaryExpression(int line, String operator, JExpression arg) {
        super(line);
        this.operator = operator;
        this.arg = arg;
    }

}

/**
 * The AST node for a unary negation (-) expression.
 */

class JNegateOp extends JUnaryExpression {

    /**
     * Constructs an AST node for a negation expression given its line number,
     * and the operand.
     * 
     * @param line
     *            line in which the negation expression occurs in the source
     *            file.
     * @param arg
     *            the operand.
     */

    public JNegateOp(int line, JExpression arg) {
        super(line, "-", arg);
    }

    /**
     * Analyzing the negation operation involves analyzing its operand, checking
     * its type and determining the result type.
     * 
     * @param context
     *            context in which names are resolved.
     * @return the analyzed (and possibly rewritten) AST subtree.
     */

    public JExpression analyze(Context context) {
        operand = operand.analyze(context);
        if (operand.type() == Type.INT) {
            type = Type.INT;
        } else if (operand.type() == Type.DOUBLE) {
            type = Type.DOUBLE;
        } else if (operand.type() == Type.LONG) {
            type = Type.LONG;
        } else {
            type = Type.ANY;
            JAST.compilationUnit.reportSemanticError(line(), "Invalid operand types for -");
        }
        return this;
    }

    /**
     * Generating code for the negation operation involves generating code for
     * the operand, and then the negation instruction.
     * 
     * @param output
     *            the code emitter (basically an abstraction for producing the
     *            .class file).
     */

    public void codegen(CLEmitter output) {
        operand.codegen(output);
        if (operand.type() == Type.INT) {
            output.addNoArgInstruction(INEG);
        } else if (operand.type() == Type.DOUBLE) {
            output.addNoArgInstruction(DNEG);
        } else if (operand.type() == Type.LONG) {
            output.addNoArgInstruction(LNEG);
        }
    }
}

/**
 * The AST node for a logical NOT (!) expression.
 */

class JLogicalNotOp extends JUnaryExpression {

    /**
     * Constructs an AST for a logical NOT expression given its line number, and
     * the operand.
     * 
     * @param line
     *            line in which the logical NOT expression occurs in the source
     *            file.
     * @param arg
     *            the operand.
     */

    public JLogicalNotOp(int line, JExpression arg) {
        super(line, "!", arg);
    }

    /**
     * Analyzing a logical NOT operation means analyzing its operand, insuring
     * it's a boolean, and setting the result to boolean.
     * 
     * @param context
     *            context in which names are resolved.
     */

    public JExpression analyze(Context context) {
        arg = (JExpression) arg.analyze(context);
        arg.type().mustMatchExpected(line(), Type.BOOLEAN);
        type = Type.BOOLEAN;
        return this;
    }

    /**
     * Generates code for the case where we actually want a boolean value 
     * ({@code true} or {@code false}) computed onto the stack. For example,
     * assignment to a boolean variable.
     * 
     * @param output
     *            the code emitter (basically an abstraction for producing the
     *            .class file).
     */

    public void codegen(CLEmitter output) {
        String elseLabel = output.createLabel();
        String endIfLabel = output.createLabel();
        this.codegen(output, elseLabel, false);
        output.addNoArgInstruction(ICONST_1); // true
        output.addBranchInstruction(GOTO, endIfLabel);
        output.addLabel(elseLabel);
        output.addNoArgInstruction(ICONST_0); // false
        output.addLabel(endIfLabel);
    }

    /**
     * The code generation necessary for branching simply flips the condition on
     * which we branch.
     * 
     * @param output
     *            the code emitter (basically an abstraction for producing the
     *            .class file).
     */

    public void codegen(CLEmitter output, String targetLabel, boolean onTrue) {
        arg.codegen(output, targetLabel, !onTrue);
    }

}
class JPreDecrementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a pre-decrement expression.
     *
     * @param line    line in which the expression occurs in the source file.
     * @param operand the operand.
     */
    public JPreDecrementOp(int line, JExpression operand) {
        super(line, "-- (pre)", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to -- must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            if (operand.type().equals(Type.INT)) {
                operand.type().mustMatchExpected(line(), Type.INT);
                type = Type.INT;
            } else if (operand.type().equals(Type.LONG)) {
                operand.type().mustMatchExpected(line(), Type.LONG);
                type = Type.LONG;
            } else if (operand.type().equals(Type.DOUBLE)) {
                operand.type().mustMatchExpected(line(), Type.DOUBLE);
                type = Type.DOUBLE;
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            output.addIINCInstruction(offset, -1);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            output.addNoArgInstruction(ICONST_1);
            if (operand.type().equals(Type.INT)) {
                output.addNoArgInstruction(ISUB);
            } else if (operand.type().equals(Type.LONG)) {
                output.addNoArgInstruction(LSUB);
            } else if (operand.type().equals(Type.DOUBLE)) {
                output.addNoArgInstruction(DSUB);
            }
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}

/**
 * The AST node for an expr--.
 */

class JPostDecrementOp extends JUnaryExpression {

    /**
     * Constructs an AST node for an expr-- expression given its line number, and
     * the operand.
     * 
     * @param line
     *            line in which the expression occurs in the source file.
     * @param arg
     *            the operand.
     */

    public JPostDecrementOp(int line, JExpression arg) {
        super(line, "post--", arg);
    }

    /**
     * Analyzes the operand as a lhs (since there is a side effect), checks types
     * and determines the type of the result.
     * 
     * @param context
     *            context in which names are resolved.
     * @return the analyzed (and possibly rewritten) AST subtree.
     */

    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to -- must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            if (operand.type().equals(Type.INT)) {
                operand.type().mustMatchExpected(line(), Type.INT);
                type = Type.INT;
            } else if (operand.type().equals(Type.LONG)) {
                operand.type().mustMatchExpected(line(), Type.LONG);
                type = Type.LONG;
            } else if (operand.type().equals(Type.DOUBLE)) {
                operand.type().mustMatchExpected(line(), Type.DOUBLE);
                type = Type.DOUBLE;
            }
        }
        return this;
    }

    /**
     * In generating code for a post-decrement operation, we treat simple
     * variable ({@link JVariable}) operands specially since the JVM has an 
     * increment instruction. 
     * Otherwise, we rely on the {@link JLhs} code generation support for
     * generating the proper code. Notice that we distinguish between
     * expressions that are statement expressions and those that are not; we
     * insure the proper value (before the decrement) is left atop the stack in
     * the latter case.
     * 
     * @param output
     *            the code emitter (basically an abstraction for producing the
     *            .class file).
     */

    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
            output.addIINCInstruction(offset, -1);
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            output.addNoArgInstruction(ICONST_1);
            if (operand.type().equals(Type.INT)) {
                output.addNoArgInstruction(ISUB);
            } else if (operand.type().equals(Type.LONG)) {
                output.addNoArgInstruction(LSUB);
            } else if (operand.type().equals(Type.DOUBLE)) {
                output.addNoArgInstruction(DSUB);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}



/**
 * The AST node for a ++expr expression.
 */

class JPreIncrementOp extends JUnaryExpression {

    /**
     * Constructs an AST node for a ++expr given its line number, and the
     * operand.
     * 
     * @param line
     *            line in which the expression occurs in the source file.
     * @param arg
     *            the operand.
     */

    public JPreIncrementOp(int line, JExpression arg) {
        super(line, "++pre", arg);
    }

    /**
     * Analyzes the operand as a lhs (since there is a side effect), check types
     * and determine the type of the result.
     * 
     * @param context
     *            context in which names are resolved.
     * @return the analyzed (and possibly rewritten) AST subtree.
     */

    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to ++ must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            if (operand.type().equals(Type.INT)) {
                operand.type().mustMatchExpected(line(), Type.INT);
                type = Type.INT;
            } else if (operand.type().equals(Type.LONG)) {
                operand.type().mustMatchExpected(line(), Type.LONG);
                type = Type.LONG;
            } else if (operand.type().equals(Type.DOUBLE)) {
                operand.type().mustMatchExpected(line(), Type.DOUBLE);
                type = Type.DOUBLE;
            }
        }
        return this;
    }


    /**
     * In generating code for a pre-increment operation, we treat simple
     * variable ({@link JVariable}) operands specially since the JVM has an 
     * increment instruction. 
     * Otherwise, we rely on the {@link JLhs} code generation support for
     * generating the proper code. Notice that we distinguish between
     * expressions that are statement expressions and those that are not; we
     * insure the proper value (after the increment) is left atop the stack in
     * the latter case.
     * 
     * @param output
     *            the code emitter (basically an abstraction for producing the
     *            .class file).
     */

    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            output.addIINCInstruction(offset, 1);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            output.addNoArgInstruction(ICONST_1);
            if (operand.type().equals(Type.INT)) {
                output.addNoArgInstruction(IADD);
            } else if (operand.type().equals(Type.LONG)) {
                output.addNoArgInstruction(LADD);
            } else if (operand.type().equals(Type.DOUBLE)) {
                output.addNoArgInstruction(DADD);
            }
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}

    class JPostIncrementOp extends JUnaryExpression {

    /**
     * Constructs an AST node for a post-increment operation.
     *
     * @param line line number in which the post-increment occurs in the source file.
     * @param arg  the operand being incremented.
     */
    public JPostIncrementOp(int line, JExpression arg) {
        super(line, "++post", arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to ++ must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            if (operand.type().equals(Type.INT)) {
                operand.type().mustMatchExpected(line(), Type.INT);
                type = Type.INT;
            } else if (operand.type().equals(Type.LONG)) {
                operand.type().mustMatchExpected(line(), Type.LONG);
                type = Type.LONG;
            } else if (operand.type().equals(Type.DOUBLE)) {
                operand.type().mustMatchExpected(line(), Type.DOUBLE);
                type = Type.DOUBLE;
            }
        }
        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
            output.addIINCInstruction(offset, 1);
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            output.addNoArgInstruction(ICONST_1);
            if (operand.type().equals(Type.INT)) {
                output.addNoArgInstruction(IADD);
            } else if (operand.type().equals(Type.LONG)) {
                output.addNoArgInstruction(LADD);
            } else if (operand.type().equals(Type.DOUBLE)) {
                output.addNoArgInstruction(DADD);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}
