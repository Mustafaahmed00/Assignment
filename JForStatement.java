package jminusminus;

import java.util.ArrayList;

/**
 * The AST node for a for-statement.
 */
class JForStatement extends JStatement {
    // Initialization.
    private ArrayList<JStatement> init;

    // Test expression
    private JExpression condition;

    // Update.
    private ArrayList<JStatement> update;

    // The body.
    private JStatement body;

    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param init      the initialization.
     * @param condition the test expression.
     * @param update    the update.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> init, JExpression condition, ArrayList<JStatement> update,
                         JStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context) {
    if (init != null) {
        for (int i = 0; i < init.size(); i++) {
            init.set(i, (JStatement) init.get(i).analyze(context));
        }
    }

    if (condition != null) {
        condition = (JExpression) condition.analyze(context);
        condition.type().mustMatchExpected(line(), Type.BOOLEAN);
    }

    if (update != null) {
        for (int i = 0; i < update.size(); i++) {
            update.set(i, (JStatement) update.get(i).analyze(context));
        }
    }

    if (body != null) {
        body = (JStatement) body.analyze(context);
    }

    return this;
}


    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
    String startLabel = output.createLabel();
    String conditionLabel = output.createLabel();
    String updateLabel = output.createLabel();
    String endLabel = output.createLabel();

    // Generate code for initialization statements
    if (init != null) {
        for (JStatement statement : init) {
            statement.codegen(output);
        }
    }

    // Jump to condition check
    output.addBranchInstruction(CLEmitter.GOTO, conditionLabel);

    // Label for loop body start
    output.addLabel(startLabel);

    // Generate code for loop body
    if (body != null) {
        body.codegen(output);
    }

    // Generate code for update statements
    output.addLabel(updateLabel);
    if (update != null) {
        for (JStatement statement : update) {
            statement.codegen(output);
        }
    }

    // Label for condition check
    output.addLabel(conditionLabel);
    if (condition != null) {
        condition.codegen(output, endLabel, false);
    } else {
        // If no condition, always jump to startLabel
        output.addBranchInstruction(CLEmitter.GOTO, startLabel);
    }

    // Jump back to loop body start
    output.addBranchInstruction(CLEmitter.GOTO, startLabel);

    // Label for loop end
    output.addLabel(endLabel);
}


    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (init != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement stmt : init) {
                stmt.toJSON(e1);
            }
        }
        if (condition != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Condition", e1);
            condition.toJSON(e1);
        }
        if (update != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Update", e1);
            for (JStatement stmt : update) {
                stmt.toJSON(e1);
            }
        }
        if (body != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Body", e1);
            body.toJSON(e1);
        }
    }
}
