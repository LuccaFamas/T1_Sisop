package simulation;

import cpu.ExecutionResult;
import process.PCB;
import process.ProcessState;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Trata o que acontece depois de uma instrução: syscalls e erros.
// O pc NÃO é mexido aqui; isso é só do InstructionExecutor.
public class OperatingSystem {

    // SYSCALL 1/2 no tick t: bloqueado em t+1, t+2, t+3; pronto em t+4
    private static final int IO_BLOCK_TICKS = 3;

    // Um único leitor do teclado para o programa inteiro: a SYSCALL 2 e o
    // painel (Enter) leem daqui. Dois Scanner no System.in se atrapalhariam.
    static final Scanner KEYBOARD = new Scanner(System.in);

    // Mensagens das syscalls, para o painel mostrar de novo
    private final List<String> log = new ArrayList<>();

    public List<String> getLog() {
        return log;
    }

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

        say("[" + pcb.getName() + "] Erro de execução: " + message);

        pcb.setEndedWithError(true);
        pcb.setState(ProcessState.FINISHED);
    }

    private void handleHalt(PCB pcb) {

        say("[" + pcb.getName() + "] Finalizado (SYSCALL 0)");

        pcb.setState(ProcessState.FINISHED);
    }

    // Imprime no tick em que a SYSCALL executa (não no desbloqueio)
    private void handlePrint(PCB pcb, int currentTime) {

        say("[" + pcb.getName() + "] Impressão (SYSCALL 1): "
            + pcb.getAcc());

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
            if (!KEYBOARD.hasNextLine()) {
                System.out.println();
                handleExecutionError(pcb, "entrada encerrada durante SYSCALL 2");
                return;
            }

            String line = KEYBOARD.nextLine().trim();

            try {
                pcb.setAcc(Integer.parseInt(line));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido: \"" + line + "\"");
            }
        }

        say("[" + pcb.getName() + "] Lido (SYSCALL 2): " + pcb.getAcc());

        blockProcess(pcb, currentTime);
    }

    private void say(String message) {
        System.out.println(message);
        log.add(message);
    }

    private void blockProcess(PCB pcb, int currentTime) {

        pcb.setState(ProcessState.BLOCKED);
        pcb.setBlockedUntil(currentTime + IO_BLOCK_TICKS + 1);
    }
}
