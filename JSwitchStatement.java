package jminusminus;

import java.util.ArrayList;

/**
 * The AST node for a switch-statement.
 */
class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // List of switch-statement groups.
    private ArrayList<SwitchStatementGroup> switchStmtGroups;

    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line             line in which the switch-statement occurs in the source file.
     * @param condition        test expression.
     * @param switchStmtGroups list of statement groups.
     */
    public JSwitchStatement(int line, JExpression condition, ArrayList<SwitchStatementGroup> switchStmtGroups) {
        super(line);
        this.condition = condition;
        this.switchStmtGroups = switchStmtGroups;
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
    // Analyze the switch expression
    condition = (JExpression) condition.analyze(context);
    Type condType = condition.type();

    // Switch expression must be char, byte, short, int, or String (for simplicity, we'll handle int)
    if (condType != Type.INT) {
        JAST.compilationUnit.reportSemanticError(line(), 
            "Switch expression must be of type int.");
    }

    // Set to keep track of case label values to detect duplicates
    Set<Integer> caseValues = new HashSet<>();
    boolean hasDefault = false;

    // Analyze each switch statement group
    for (SwitchStatementGroup group : switchStmtGroups) {
        group.analyze(context, condType, caseValues);
        // Check for default label
        if (group.hasDefaultLabel()) {
            if (hasDefault) {
                JAST.compilationUnit.reportSemanticError(line(), 
                    "Duplicate default label in switch.");
            }
            hasDefault = true;
        }
    }

    return this;
}


    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
    // Generate code for the switch expression
    condition.codegen(output);

    // Collect case values and their corresponding labels
    Map<Integer, String> caseLabels = new HashMap<>();
    String defaultLabel = null;
    String endLabel = output.createLabel();

    // First pass to collect labels
    for (SwitchStatementGroup group : switchStmtGroups) {
        for (JExpression label : group.getSwitchLabels()) {
            if (label != null) {
                // Case label
                Integer value = (Integer) label.constant().value();
                String caseLabel = output.createLabel();
                caseLabels.put(value, caseLabel);
            } else {
                // Default label
                if (defaultLabel == null) {
                    defaultLabel = output.createLabel();
                }
            }
        }
    }

    if (defaultLabel == null) {
        defaultLabel = endLabel; // If no default, point to end of switch
    }

    // Generate the switch instruction
    ArrayList<Integer> keys = new ArrayList<>(caseLabels.keySet());
    Collections.sort(keys);
    ArrayList<String> labels = new ArrayList<>();
    for (Integer key : keys) {
        labels.add(caseLabels.get(key));
    }

    output.addTableSwitchInstruction(keys, labels, defaultLabel);

    // Generate code for each case
    int groupIndex = 0;
    for (SwitchStatementGroup group : switchStmtGroups) {
        for (JExpression label : group.getSwitchLabels()) {
            String labelName;
            if (label != null) {
                // Case label
                Integer value = (Integer) label.constant().value();
                labelName = caseLabels.get(value);
            } else {
                // Default label
                labelName = defaultLabel;
            }
            output.addLabel(labelName);

            // Generate code for statements in the block
            for (JStatement stmt : group.block()) {
                stmt.codegen(output);
            }
        }
    }

    output.addLabel(endLabel);
}


    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchStatementGroup group : switchStmtGroups) {
            group.toJSON(e);
        }
    }
}

/**
 * A switch-statement group consists of a list of switch labels and a block of statements.
 */
class SwitchStatementGroup {
    // Switch labels.
    private ArrayList<JExpression> switchLabels;

    // Block of statements.
    private ArrayList<JStatement> block;

    /**
     * Constructs a switch-statement group.
     *
     * @param switchLabels switch labels.
     * @param block        block of statements.
     */
    public SwitchStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> block) {
        this.switchLabels = switchLabels;
        this.block = block;
    }

    /**
     * Returns the switch labels associated with this switch-statement group.
     *
     * @return the switch labels associated with this switch-statement group.
     */
    public ArrayList<JExpression> getSwitchLabels() {
        return switchLabels;
    }

    /**
     * Returns the block of statements associated with this switch-statement group.
     *
     * @return the block of statements associated with this switch-statement group.
     */
    public ArrayList<JStatement> block() {
        return block;
    }

    /**
     * Stores information about this switch statement group in JSON format.
     *
     * @param json the JSON emitter.
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("SwitchStatementGroup", e);
        for (JExpression label : switchLabels) {
            JSONElement e1 = new JSONElement();
            if (label != null) {
                e.addChild("Case", e1);
                label.toJSON(e1);
            } else {
                e.addChild("Default", e1);
            }
        }
        if (block != null) {
            for (JStatement stmt : block) {
                stmt.toJSON(e);
            }
        }
    }
}
