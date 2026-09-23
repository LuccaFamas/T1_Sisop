package assembly;

public class Instruction {

    private String operation;
    private String operand;

    public Instruction(String operation, String operand) {
        this.operation = operation;
        this.operand = operand;
    }

    public String getOperation() {
        return operation;
    }

    public String getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        if (operand == null) {
            return operation;
        }

        return operation + " " + operand;
    }
}