package simulation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import assembly.Instruction;
import cpu.ExecutionException;
import cpu.ExecutionResult;
import cpu.InstructionExecutor;
import process.PCB;
import process.PriorityGroupQueue;
import process.ProcessState;

// Relógio e laço de ticks do MLFQ.
// Cada tick t segue SEMPRE esta ordem:
//   1. desbloqueio   2. admissão   3. preempção por interrupção
//   4. dispatch      5. snapshot   6. execução   7. pós-execução
public class Scheduler {

    public static final int QUANTUM_QUEUE_0 = 2;
    public static final int QUANTUM_QUEUE_1 = 4;

    // Proteção contra programa em loop infinito
    public static final int MAX_TICKS = 10000;

    private final List<PCB> processes;

    private final ArrayDeque<PCB> queue0;
    private final PriorityGroupQueue queue1;
    private final List<PCB> blocked;

    private PCB running;
    private int time;

    private final InstructionExecutor executor;
    private final OperatingSystem os;
    private final Monitor monitor;

    private final List<TickSnapshot> history;

    public Scheduler(List<PCB> processes, Monitor monitor) {

        // Ordem crescente de nome: desempate determinístico quando
        // vários processos chegam/desbloqueiam no mesmo tick
        this.processes = new ArrayList<>(processes);
        this.processes.sort(Comparator.comparing(PCB::getName));

        this.queue0 = new ArrayDeque<>();
        this.queue1 = new PriorityGroupQueue();
        this.blocked = new ArrayList<>();

        this.running = null;
        this.time = 0;

        this.executor = new InstructionExecutor();
        this.os = new OperatingSystem();
        this.monitor = monitor;

        this.history = new ArrayList<>();
    }

    public List<PCB> getProcesses() {
        return processes;
    }

    public List<TickSnapshot> getHistory() {
        return history;
    }

    // Devolve false se parou pelo limite de segurança.
    // tickDelayMillis: pausa real entre ticks, só para acompanhar a saída
    // (0 = sem pausa). Não muda nada no resultado da simulação.
    public boolean run(int tickDelayMillis) {

        while (!allFinished()) {

            if (time >= MAX_TICKS) {
                return false;
            }

            tick();
            time++;

            if (tickDelayMillis > 0 && !allFinished()) {
                pause(tickDelayMillis);
            }
        }

        return true;
    }

    private void pause(int millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Mantém o sinal de interrupção; a simulação segue sem pausa
            Thread.currentThread().interrupt();
        }
    }

    private void tick() {

        List<String> preEvents = new ArrayList<>();

        // 1. Desbloqueio: final da Fila 0 (antes das chegadas novas)
        for (PCB pcb : processes) {

            if (pcb.getState() == ProcessState.BLOCKED
                    && pcb.getBlockedUntil() == time) {

                blocked.remove(pcb);
                enterQueue0(pcb);
            }
        }

        // 2. Admissão: final da Fila 0
        for (PCB pcb : processes) {

            if (pcb.getState() == ProcessState.NEW
                    && pcb.getArrivalTime() == time) {

                enterQueue0(pcb);
            }
        }

        // 3. Preempção por interrupção: só um processo da Fila 1 é
        // interrompido. Volta ao topo do grupo e mantém o quantum.
        if (running != null
                && running.getQueueLevel() == 1
                && !queue0.isEmpty()) {

            running.setState(ProcessState.READY);
            queue1.addFirstOfGroup(running);
            preEvents.add(running.getName() + " preemptado -> topo da Fila 1 (quantum "
                + running.getQuantumRemaining() + ")");
            running = null;
        }

        // 4. Dispatch: Fila 0 tem prioridade sobre a Fila 1
        if (running == null) {

            running = queue0.pollFirst();

            if (running == null) {
                running = queue1.poll();
            }

            if (running != null) {
                running.setState(ProcessState.RUNNING);
            }
        }

        // 5. Snapshot
        TickSnapshot snapshot = takeSnapshot();

        for (String event : preEvents) {
            snapshot.addEvent(event);
        }

        history.add(snapshot);

        // 6. Execução. Contadores de espera e I/O pelo estado DURANTE
        // o tick, antes de a instrução mudar o estado de alguém.
        for (PCB pcb : processes) {

            if (pcb.getState() == ProcessState.READY) {
                pcb.addWaitTime();
            } else if (pcb.getState() == ProcessState.BLOCKED) {
                pcb.addIoTime();
            }
        }

        if (running != null) {
            executeRunning(snapshot);
        }

        monitor.printTick(snapshot);
    }

    private void executeRunning(TickSnapshot snapshot) {

        PCB pcb = running;
        String instructionText = currentInstructionText(pcb);

        ExecutionResult result = null;

        try {
            result = executor.execute(pcb);
            os.handleExecutionResult(result, pcb, time);
        } catch (ExecutionException e) {
            os.handleExecutionError(pcb, e.getMessage());
        }

        pcb.addCpuTime();
        pcb.setQuantumRemaining(pcb.getQuantumRemaining() - 1);

        snapshot.setExecution(instructionText, pcb.getAcc());

        // 7. Pós-execução. Precedência: halt/erro > I/O > quantum

        if (pcb.getState() == ProcessState.FINISHED) {

            pcb.setFinishTime(time + 1);
            snapshot.addEvent((pcb.hasEndedWithError() ? "erro" : "halt")
                + ", término " + pcb.getFinishTime());
            running = null;
            return;
        }

        if (pcb.getState() == ProcessState.BLOCKED) {

            blocked.add(pcb);
            snapshot.addEvent(result == ExecutionResult.SYSCALL_PRINT
                ? "imprime " + pcb.getAcc()
                : "lê " + pcb.getAcc());
            running = null;
            return;
        }

        if (pcb.getQuantumRemaining() == 0) {

            // Fila 0 é rebaixada; Fila 1 vai ao final do seu grupo
            snapshot.addEvent(pcb.getQueueLevel() == 0
                ? "quantum -> Fila 1"
                : "quantum -> final do grupo na Fila 1");

            pcb.setQueueLevel(1);
            pcb.setQuantumRemaining(QUANTUM_QUEUE_1);
            pcb.setState(ProcessState.READY);
            queue1.addLastOfGroup(pcb);
            running = null;
        }
    }

    private void enterQueue0(PCB pcb) {

        pcb.setState(ProcessState.READY);
        pcb.setQueueLevel(0);
        pcb.setQuantumRemaining(QUANTUM_QUEUE_0);
        queue0.addLast(pcb);
    }

    private boolean allFinished() {

        for (PCB pcb : processes) {

            if (pcb.getState() != ProcessState.FINISHED) {
                return false;
            }
        }

        return true;
    }

    private String currentInstructionText(PCB pcb) {

        List<Instruction> instructions = pcb.getProgram().getInstructions();
        int pc = pcb.getPc();

        if (pc < 0 || pc >= instructions.size()) {
            return "(fora do programa)";
        }

        return instructions.get(pc).toString();
    }

    private TickSnapshot takeSnapshot() {

        List<String> queue0Names = new ArrayList<>();

        for (PCB pcb : queue0) {
            queue0Names.add(pcb.getName());
        }

        List<String> queue1Names = new ArrayList<>();

        for (PCB pcb : queue1.toList()) {
            queue1Names.add(pcb.getName() + "(" + pcb.getPriority() + ")");
        }

        // Bloqueados em ordem de desbloqueio, depois de nome
        List<PCB> blockedSorted = new ArrayList<>(blocked);
        blockedSorted.sort(Comparator.comparingInt(PCB::getBlockedUntil)
            .thenComparing(PCB::getName));

        List<String> blockedNames = new ArrayList<>();

        for (PCB pcb : blockedSorted) {
            blockedNames.add(pcb.getName() + "->" + pcb.getBlockedUntil());
        }

        Map<String, ProcessState> states = new LinkedHashMap<>();

        for (PCB pcb : processes) {
            states.put(pcb.getName(), pcb.getState());
        }

        return new TickSnapshot(
            time,
            running == null ? null : running.getName(),
            queue0Names,
            queue1Names,
            blockedNames,
            states);
    }
}
