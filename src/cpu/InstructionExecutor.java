package cpu;

import assembly.Instruction;
import assembly.InstructionType;
import process.PCB;

import java.util.List;

// Executa UMA instrução de um processo.
// Sem estado: memória, labels, pc e acc vêm todos do PCB.
public class InstructionExecutor {

    public ExecutionResult execute(PCB pcb) throws ExecutionException {

        List<Instruction> instructions = pcb.getProgram().getInstructions();
        int pc = pcb.getPc();

        // Programa sem SYSCALL 0 no fim, ou salto para fora
        if (pc < 0 || pc >= instructions.size()) {
            throw new ExecutionException(
                "PC fora do programa: " + pc
            );
        }

        Instruction instruction = instructions.get(pc);
        InstructionType type = instruction.getType();
        String operand = instruction.getOperand();

        // Único lugar onde o pc avança. Os saltos, quando tomados,
        // sobrescrevem este valor logo abaixo.
        pcb.setPc(pc + 1);

        switch (type) {

            case LOAD:
                pcb.setAcc(resolveOperand(operand, pcb));
                return ExecutionResult.CONTINUE;

            case STORE:
                pcb.getMemory().set(operand, pcb.getAcc());
                return ExecutionResult.CONTINUE;

            // Aritmética em int do Java: MULT (e ADD/SUB) pode estourar
            // e "dar a volta" sem aviso, como numa CPU real de 32 bits.
            case ADD:
                pcb.setAcc(pcb.getAcc() + resolveOperand(operand, pcb));
                return ExecutionResult.CONTINUE;

            case SUB:
                pcb.setAcc(pcb.getAcc() - resolveOperand(operand, pcb));
                return ExecutionResult.CONTINUE;

            case MULT:
                pcb.setAcc(pcb.getAcc() * resolveOperand(operand, pcb));
                return ExecutionResult.CONTINUE;

            // Divisão inteira: trunca em direção a zero (-7 / 2 = -3).
            case DIV:
                int divisor = resolveOperand(operand, pcb);

                if (divisor == 0) {
                    throw new ExecutionException("divisão por zero");
                }

                pcb.setAcc(pcb.getAcc() / divisor);
                return ExecutionResult.CONTINUE;

            case BRANY:
                jump(operand, pcb);
                return ExecutionResult.CONTINUE;

            case BRPOS:
                if (pcb.getAcc() > 0) {
                    jump(operand, pcb);
                }
                return ExecutionResult.CONTINUE;

            case BRZERO:
                if (pcb.getAcc() == 0) {
                    jump(operand, pcb);
                }
                return ExecutionResult.CONTINUE;

            case BRNEG:
                if (pcb.getAcc() < 0) {
                    jump(operand, pcb);
                }
                return ExecutionResult.CONTINUE;

            // O parser garante que o operando é 0, 1 ou 2
            case SYSCALL:
                if (operand.equals("0")) {
                    return ExecutionResult.SYSCALL_HALT;
                }
                if (operand.equals("1")) {
                    return ExecutionResult.SYSCALL_PRINT;
                }
                return ExecutionResult.SYSCALL_READ;

            default:
                throw new ExecutionException(
                    "instrução não suportada: " + type
                );
        }
    }

    private int resolveOperand(String operand, PCB pcb) {

        // Modo imediato
        // Exemplo: #5
        if (operand.startsWith("#")) {
            return Integer.parseInt(operand.substring(1));
        }

        // Modo direto
        // Exemplo: valor
        return pcb.getMemory().get(operand);
    }

    // O parser garante que o label existe
    private void jump(String label, PCB pcb) {
        pcb.setPc(pcb.getProgram().getLabels().get(label));
    }
}
