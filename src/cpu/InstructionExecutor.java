package cpu;

import java.util.Map;

import assembly.Instruction;
import process.Process;

public class InstructionExecutor {

    private Memory memory;
    private Map<String, Integer> labels;

    public InstructionExecutor(
            Memory memory,
            Map<String, Integer> labels) {
        this.memory = memory;
        this.labels = labels;
    }

    public void execute(Instruction instruction, Process process) {

        String operation = instruction.getOperation();
        String operand = instruction.getOperand();

        switch (operation) {

            case "LOAD":
                executeLoad(process, operand);
                break;

            case "STORE":
                executeStore(process, operand);
                break;

            case "ADD":
                executeAdd(process, operand);
                break;

            case "SUB":
                executeSub(process, operand);
                break;

            case "MULT":
                executeMult(process, operand);
                break;

            case "DIV":
                executeDiv(process, operand);
                break;

            case "BRANY":
                executeBrany(process, operand);
                break;

            case "BRPOS":
                executeBrpos(process, operand);
                break;

            case "BRZERO":
                executeBrzero(process, operand);
                break;

            case "BRNEG":
                executeBrneg(process, operand);
                break;

            default:
                throw new IllegalArgumentException(
                        "Operação desconhecida: " + operation);
        }
    }

    private void executeLoad(Process process, String operand) {

        int value = resolveOperand(operand);

        process.setAcc(value);
        process.setPc(process.getPc() + 1);
    }

    private void executeStore(Process process, String operand) {

        memory.set(operand, process.getAcc());

        process.setPc(process.getPc() + 1);
    }

    private void executeAdd(Process process, String operand) {

        int value = resolveOperand(operand);

        process.setAcc(process.getAcc() + value);
        process.setPc(process.getPc() + 1);
    }

    private void executeSub(Process process, String operand) {

        int value = resolveOperand(operand);

        process.setAcc(process.getAcc() - value);
        process.setPc(process.getPc() + 1);
    }

    private void executeMult(Process process, String operand) {

        int value = resolveOperand(operand);

        process.setAcc(process.getAcc() * value);
        process.setPc(process.getPc() + 1);
    }

    private void executeDiv(Process process, String operand) {

        int value = resolveOperand(operand);

        if (value == 0) {
            throw new ArithmeticException("Divisão por zero");
        }

        process.setAcc(process.getAcc() / value);
        process.setPc(process.getPc() + 1);
    }

    private int resolveOperand(String operand) {

        if (operand.startsWith("#")) {

            return Integer.parseInt(
                    operand.substring(1));
        }

        return memory.get(operand);
    }

    private void executeBrany(
            Process process,
            String label) {
        process.setPc(getLabelPosition(label));
    }

    private void executeBrpos(
            Process process,
            String label) {
        if (process.getAcc() > 0) {
            process.setPc(getLabelPosition(label));
        } else {
            process.setPc(process.getPc() + 1);
        }
    }

    private void executeBrneg(
            Process process,
            String label) {
        if (process.getAcc() < 0) {
            process.setPc(getLabelPosition(label));
        } else {
            process.setPc(process.getPc() + 1);
        }
    }

    private void executeBrzero(
            Process process,
            String label) {
        if (process.getAcc() == 0) {
            process.setPc(getLabelPosition(label));
        } else {
            process.setPc(process.getPc() + 1);
        }
    }

    private int getLabelPosition(String label) {

        if (!labels.containsKey(label)) {
            throw new IllegalArgumentException(
                    "Label não encontrado: " + label);
        }

        return labels.get(label);
    }

}