package process;

public class Process {

    private String name;
    private int arrivalTime;
    private int priority;

    private int pc;
    private int acc;

    private ProcessState state;

    public Process(String name, int arrivalTime, int priority) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.priority = priority;

        this.pc = 0;
        this.acc = 0;

        this.state = ProcessState.NEW;
    }

public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    BLOCKED,
    FINISHED
}

public int getAcc() {
   return acc;
}

public void setAcc(int acc) {
    this.acc = acc;
}

public int getPc() {
    return pc;
}

public void setPc(int pc) {
    this.pc = pc;
}
}