package simulation;

import cpu.ExecutionResult;
import process.PCB;
import process.ProcessState;

import java.util.Scanner;

// Trata o que acontece depois de uma instrução: syscalls e erros.
// O pc NÃO é mexido aqui; isso é só do InstructionExecutor.
public class OperatingSystem {

    // SYSCALL 1/2 no tick t: bloqueado em t+1, t+2, t+3; pronto em t+4
    private static final int IO_BLOCK_TICKS = 3;

    private final Scanner keyboard = new Scanner(System.in);

    public void handleExecutionResult(
            ExecutionResult result,
            PCB pcb,
            int currentTime) {

        switch (result) {

            case CONTINUE:
                break;

            case SYSCALL_HALT:
                handleHalt(pcb);
                break;

            case SYSCALL_PRINT:
                handlePrint(pcb, currentTime);
                break;

            case SYSCALL_READ:
                handleRead(pcb, currentTime);
                break;
        }
    }

    // Opção A: erro de execução encerra o processo como um halt,
    // só que marcado como erro.
    public void handleExecutionError(PCB pcb, String message) {

        System.out.println(
            "[" + pcb.getName() + "] Erro de execução: " + message
        );

        pcb.setEndedWithError(true);
        pcb.setState(ProcessState.FINISHED);
    }

    private void handleHalt(PCB pcb) {

        System.out.println(
            "[" + pcb.getName() + "] Finalizado (SYSCALL 0)"
        );

        pcb.setState(ProcessState.FINISHED);
    }

    // Imprime no tick em que a SYSCALL executa (não no desbloqueio)
    private void handlePrint(PCB pcb, int currentTime) {

        System.out.println(
            "[" + pcb.getName() + "] Impressão (SYSCALL 1): "
            + pcb.getAcc()
        );

        blockProcess(pcb, currentTime);
    }

    // Lê no tick em que a SYSCALL executa, repetindo até receber
    // um inteiro válido.
    private void handleRead(PCB pcb, int currentTime) {

        while (true) {

            System.out.print(
                "[" + pcb.getName() + "] Leitura (SYSCALL 2): digite um inteiro: "
            );

            // Fim da entrada (Ctrl+Z/Ctrl+D ou arquivo redirecionado acabou):
            // sem isso o laço nunca terminaria.
            if (!keyboard.hasNextLine()) {
                System.out.println();
                handleExecutionError(pcb, "entrada encerrada durante SYSCALL 2");
                return;
            }

            String line = keyboard.nextLine().trim();

            try {
                pcb.setAcc(Integer.parseInt(line));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido: \"" + line + "\"");
            }
        }

        System.out.println(
            "[" + pcb.getName() + "] Lido (SYSCALL 2): " + pcb.getAcc()
        );

        blockProcess(pcb, currentTime);
    }

    private void blockProcess(PCB pcb, int currentTime) {

        pcb.setState(ProcessState.BLOCKED);
        pcb.setBlockedUntil(currentTime + IO_BLOCK_TICKS + 1);
    }
}
