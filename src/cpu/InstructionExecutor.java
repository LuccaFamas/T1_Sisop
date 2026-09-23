package cpu;

import assembly.Instruction;
import process.Process;

import java.util.Map;

public class InstructionExecutor {

    private Memory memory;
    private Map<String, Integer> labels;

    public InstructionExecutor(
            Memory memory,
            Map<String, Integer> labels) {

        this.memory = memory;
        this.labels = labels;
    }

    public ExecutionResult execute(
            Instruction instruction,
            Process process) {

        String operation = instruction.getOperation();
        String operand = instruction.getOperand();

        switch (operation) {

            case "LOAD":
                executeLoad(operand, process);
                return ExecutionResult.CONTINUE;

            case "STORE":
                executeStore(operand, process);
                return ExecutionResult.CONTINUE;

            case "ADD":
                executeAdd(operand, process);
                return ExecutionResult.CONTINUE;

            case "SUB":
                executeSub(operand, process);
                return ExecutionResult.CONTINUE;

            case "MULT":
                executeMult(operand, process);
                return ExecutionResult.CONTINUE;

            case "DIV":
                executeDiv(operand, process);
                return ExecutionResult.CONTINUE;

            case "BRANY":
                executeBrany(operand, process);
                return ExecutionResult.CONTINUE;

            case "BRPOS":
                executeBrpos(operand, process);
                return ExecutionResult.CONTINUE;

            case "BRZERO":
                executeBrzero(operand, process);
                return ExecutionResult.CONTINUE;

            case "BRNEG":
                executeBrneg(operand, process);
                return ExecutionResult.CONTINUE;

            case "SYSCALL":
                return executeSyscall(operand);

            default:
                throw new IllegalArgumentException(
                    "Instrução desconhecida: " + operation
                );
        }
    }

    private void executeLoad(
            String operand,
            Process process) {

        int value = resolveOperand(operand);

        process.setAcc(value);

        process.setPc(process.getPc() + 1);
    }

    private void executeStore(
            String operand,
            Process process) {

        memory.set(
            operand,
            process.getAcc()
        );

        process.setPc(process.getPc() + 1);
    }

    private void executeAdd(
            String operand,
            Process process) {

        int value = resolveOperand(operand);

        process.setAcc(
            process.getAcc() + value
        );

        process.setPc(process.getPc() + 1);
    }

    private void executeSub(
            String operand,
            Process process) {

        int value = resolveOperand(operand);

        process.setAcc(
            process.getAcc() - value
        );

        process.setPc(process.getPc() + 1);
    }

    private void executeMult(
            String operand,
            Process process) {

        int value = resolveOperand(operand);

        process.setAcc(
            process.getAcc() * value
        );

        process.setPc(process.getPc() + 1);
    }

    private void executeDiv(
            String operand,
            Process process) {

        int value = resolveOperand(operand);

        if (value == 0) {
            throw new ArithmeticException(
                "Divisão por zero"
            );
        }

        process.setAcc(
            process.getAcc() / value
        );

        process.setPc(process.getPc() + 1);
    }

    private void executeBrany(
            String operand,
            Process process) {

        process.setPc(
            getLabelPosition(operand)
        );
    }

    private void executeBrpos(
            String operand,
            Process process) {

        if (process.getAcc() > 0) {

            process.setPc(
                getLabelPosition(operand)
            );

        } else {

            process.setPc(
                process.getPc() + 1
            );
        }
    }

    private void executeBrzero(
            String operand,
            Process process) {

        if (process.getAcc() == 0) {

            process.setPc(
                getLabelPosition(operand)
            );

        } else {

            process.setPc(
                process.getPc() + 1
            );
        }
    }

    private void executeBrneg(
            String operand,
            Process process) {

        if (process.getAcc() < 0) {

            process.setPc(
                getLabelPosition(operand)
            );

        } else {

            process.setPc(
                process.getPc() + 1
            );
        }
    }

    private ExecutionResult executeSyscall(
            String operand) {

        int syscall = Integer.parseInt(operand);

        switch (syscall) {

            case 0:
                return ExecutionResult.SYSCALL_HALT;

            case 1:
                return ExecutionResult.SYSCALL_PRINT;

            case 2:
                return ExecutionResult.SYSCALL_READ;

            default:
                throw new IllegalArgumentException(
                    "SYSCALL inválido: " + syscall
                );
        }
    }

    private int resolveOperand(String operand) {

        // Modo imediato
        // Exemplo: #5
        if (operand.startsWith("#")) {

            return Integer.parseInt(
                operand.substring(1)
            );
        }

        // Modo direto
        // Exemplo: valor
        return memory.get(operand);
    }

    private int getLabelPosition(String label) {

        if (!labels.containsKey(label)) {

            throw new IllegalArgumentException(
                "Label não encontrada: " + label
            );
        }

        return labels.get(label);
    }
}