package process;

import assembly.Program;
import cpu.Memory;

// Process Control Block: tudo o que o SO guarda de um processo.
// (Chamava-se Process, mas colidia com java.lang.Process.)
public class PCB {

    private final String name;
    private final int arrivalTime;
    private final int priority;

    // Programa compartilhável; memória própria, copiada dos valores iniciais
    private final Program program;
    private final Memory memory;

    // Contexto salvo: sem classe CPU, o contexto vive direto no PCB
    private int pc;
    private int acc;

    private ProcessState state;

    // Tick em que volta a READY (bloqueio de I/O)
    private int blockedUntil;

    // Encerrado por erro de execução (divisão por zero, PC fora do programa)
    private boolean endedWithError;

    // Escalonamento: fila atual (0 ou 1) e UTs restantes do quantum
    private int queueLevel;
    private int quantumRemaining;

    // Estatísticas, em ticks
    private int cpuTime;
    private int waitTime;
    private int ioTime;
    private int finishTime;

    public PCB(String name, int arrivalTime, int priority, Program program) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.priority = priority;

        this.program = program;
        this.memory = new Memory(program.getData());

        this.pc = 0;
        this.acc = 0;

        this.state = ProcessState.NEW;

        this.blockedUntil = -1;
        this.endedWithError = false;

        this.queueLevel = 0;
        this.quantumRemaining = 0;

        this.cpuTime = 0;
        this.waitTime = 0;
        this.ioTime = 0;
        this.finishTime = -1;
    }

    public String getName() {
        return name;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getPriority() {
        return priority;
    }

    public Program getProgram() {
        return program;
    }

    public Memory getMemory() {
        return memory;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getAcc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(int blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public boolean hasEndedWithError() {
        return endedWithError;
    }

    public void setEndedWithError(boolean endedWithError) {
        this.endedWithError = endedWithError;
    }

    public int getQueueLevel() {
        return queueLevel;
    }

    public void setQueueLevel(int queueLevel) {
        this.queueLevel = queueLevel;
    }

    public int getQuantumRemaining() {
        return quantumRemaining;
    }

    public void setQuantumRemaining(int quantumRemaining) {
        this.quantumRemaining = quantumRemaining;
    }

    public int getCpuTime() {
        return cpuTime;
    }

    public void addCpuTime() {
        cpuTime++;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void addWaitTime() {
        waitTime++;
    }

    public int getIoTime() {
        return ioTime;
    }

    public void addIoTime() {
        ioTime++;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }
}
