package simulation;

import cpu.ExecutionResult;
import process.Process;
import process.ProcessState;

public class OperatingSystem {

    public void handleExecutionResult(
            ExecutionResult result,
            Process process,
            int currentTime) {

        switch (result) {

            case CONTINUE:
                break;

            case SYSCALL_HALT:
                handleHalt(process);
                break;

            case SYSCALL_PRINT:
                handlePrint(process, currentTime);
                break;

            case SYSCALL_READ:
                handleRead(process, currentTime);
                break;
        }
    }

    private void handleHalt(Process process) {

        process.setState(ProcessState.FINISHED);
    }

    private void handlePrint(
            Process process,
            int currentTime) {

        System.out.println(
            "[" + process.getName() + "] Impressão: "
            + process.getAcc()
        );

        process.setPc(
            process.getPc() + 1
        );

        blockProcess(
            process,
            currentTime
        );
    }

    private void handleRead(
            Process process,
            int currentTime) {

        System.out.println(
            "[" + process.getName() + "] Solicitou leitura"
        );

        process.setPc(
            process.getPc() + 1
        );

        blockProcess(
            process,
            currentTime
        );
    }

    private void blockProcess(
            Process process,
            int currentTime) {

        process.setState(
            ProcessState.BLOCKED
        );

        process.setBlockedUntil(
            currentTime + 3
        );
    }
}