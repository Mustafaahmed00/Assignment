package jminusminus;

/**
 * The AST node for a continue-statement.
 */
public class JContinueStatement extends JStatement {

    /**
     * Constructs an AST node for a continue-statement.
     *
     * @param line line in which the continue-statement occurs in the source file.
     */
    public JContinueStatement(int line) {
        super(line);
    }

    /**
     * Analyzes the continue statement to ensure it's within a loop.
     *
     * @param context the context in which names are resolved.
     * @return this node.
     */
    @Override
    public JContinueStatement analyze(Context context) {
        Stack<JStatement> stack = JMember.memberStack;
        if (stack.size() > 0) {
            statement = stack.pop();
            if (statement instanceof JIfStatement)
                JAST.compilationUnit.reportSemanticError(line(), "Found continue inside an if statement.");
        }
        return this;
    }

        /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (statement != null)
            output.addBranchInstruction(GOTO, statement.getContinueLabel());
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JContinueStatement:" + line, e);
    }
}