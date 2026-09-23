package process;

public class Process {

    private String name;
    private int arrivalTime;
    private int priority;

    private int pc;
    private int acc;

    private ProcessState state;

    private int blockedUntil;

    public Process(String name, int arrivalTime, int priority) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.priority = priority;

        this.pc = 0;
        this.acc = 0;

        this.state = ProcessState.NEW;

        this.blockedUntil = -1;
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
}