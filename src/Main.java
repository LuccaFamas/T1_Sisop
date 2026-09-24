import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import assembly.AssemblyParseException;
import assembly.AssemblyParser;
import assembly.Program;
import cpu.ExecutionException;
import cpu.ExecutionResult;
import cpu.InstructionExecutor;
import process.PCB;
import process.ProcessState;
import simulation.OperatingSystem;

// Fase 2: executa cada processo até o fim, instrução por instrução,
// SEM escalonador (um processo depois do outro). Será substituído
// pela carga via configuração na Fase 6.
public class Main {

    public static void main(String[] args) {

        String[] files = args;

        // Padrão: dois processos com o MESMO programa. Se a memória
        // fosse compartilhada, P02 leria valor = 15 e imprimiria 20.
        if (files.length == 0) {
            files = new String[] {
                "programas/teste1.asm",
                "programas/teste1.asm"
            };
        }

        AssemblyParser parser = new AssemblyParser();
        List<PCB> processes = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {

            try {
                Program program = parser.parseFile(Paths.get(files[i]));
                String name = String.format("P%02d", i + 1);
                processes.add(new PCB(name, 0, 3, program));
            } catch (AssemblyParseException e) {
                System.out.println("Erro de sintaxe: " + e.getMessage());
                return;
            } catch (IOException e) {
                System.out.println("Erro ao ler arquivo: " + e.getMessage());
                return;
            }
        }

        InstructionExecutor executor = new InstructionExecutor();
        OperatingSystem os = new OperatingSystem();

        for (PCB pcb : processes) {

            System.out.println("=== " + pcb.getName()
                + " (" + pcb.getProgram().getSourceName() + ")");

            int time = 0;

            while (pcb.getState() != ProcessState.FINISHED) {

                int pc = pcb.getPc();
                String text = pc < pcb.getProgram().getInstructions().size()
                    ? pcb.getProgram().getInstructions().get(pc).toString()
                    : "(fora do programa)";

                pcb.setState(ProcessState.RUNNING);

                try {
                    ExecutionResult result = executor.execute(pcb);
                    os.handleExecutionResult(result, pcb, time);
                } catch (ExecutionException e) {
                    os.handleExecutionError(pcb, e.getMessage());
                }

                System.out.println(String.format(
                    "  t=%-2d pc=%-2d %-14s -> acc=%d, próximo pc=%d, %s",
                    time, pc, text, pcb.getAcc(), pcb.getPc(), pcb.getState()));

                // Sem escalonador: o bloqueio é só registrado e o
                // processo segue direto.
                if (pcb.getState() == ProcessState.BLOCKED) {
                    pcb.setState(ProcessState.READY);
                }

                time++;
            }

            System.out.println("  memória final: "
                + pcb.getProgram().getData().keySet() + " = "
                + memoryValues(pcb)
                + (pcb.hasEndedWithError() ? "  (erro)" : ""));
            System.out.println();
        }
    }

    private static String memoryValues(PCB pcb) {

        List<Integer> values = new ArrayList<>();

        for (String name : pcb.getProgram().getData().keySet()) {
            values.add(pcb.getMemory().get(name));
        }

        return values.toString();
    }
}
