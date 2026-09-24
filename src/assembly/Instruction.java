package assembly;

public class Instruction {

    private final InstructionType type;

    // Já normalizado pelo parser:
    // imediato "#n", variável/label em minúsculas, SYSCALL "0".."2"
    private final String operand;

    // Linha no arquivo .asm, para mensagens de erro
    private final int lineNumber;

    public Instruction(
            InstructionType type,
            String operand,
            int lineNumber) {

        this.type = type;
        this.operand = operand;
        this.lineNumber = lineNumber;
    }

    public InstructionType getType() {
        return type;
    }

    public String getOperand() {
        return operand;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return type + " " + operand;
    }
}
