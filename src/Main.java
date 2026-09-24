import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import assembly.AssemblyParseException;
import assembly.AssemblyParser;
import process.PCB;
import simulation.Monitor;
import simulation.Scheduler;

// Fase 4: cenários dos gabaritos fixos no código (A, B ou C).
// Será substituído pela carga via configuração na Fase 6.
public class Main {

    public static void main(String[] args)
            throws IOException, AssemblyParseException {

        String scenario = args.length > 0 ? args[0].toUpperCase() : "C";

        AssemblyParser parser = new AssemblyParser();
        List<PCB> processes = new ArrayList<>();

        if (scenario.equals("A") || scenario.equals("C")) {
            processes.add(new PCB("P1", 0, 3,
                parser.parseFile(Paths.get("programas/teste1.asm"))));
        }

        if (scenario.equals("B") || scenario.equals("C")) {
            processes.add(new PCB("P2", 1, 5,
                parser.parseFile(Paths.get("programas/teste2.asm"))));
        }

        Monitor monitor = new Monitor();
        Scheduler scheduler = new Scheduler(processes, monitor);

        System.out.println("Cenário " + scenario);
        monitor.printHeader();

        if (!scheduler.run()) {
            System.out.println("Limite de " + Scheduler.MAX_TICKS
                + " ticks atingido: simulação interrompida");
        }

        System.out.println();

        for (PCB pcb : scheduler.getProcesses()) {

            int turnaround = pcb.getFinishTime() - pcb.getArrivalTime();

            System.out.println(pcb.getName()
                + ": término " + pcb.getFinishTime()
                + " · TA " + turnaround
                + " · CPU " + pcb.getCpuTime()
                + " · I/O " + pcb.getIoTime()
                + " · espera " + pcb.getWaitTime()
                + " (TA-CPU-I/O = " + (turnaround - pcb.getCpuTime() - pcb.getIoTime()) + ")");
        }
    }
}
