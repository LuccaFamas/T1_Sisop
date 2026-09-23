package cpu;

public class CPU {

    private int acc;
    private int pc;

    private Memory memory;

    public CPU(Memory memory) {
        this.memory = memory;
        this.acc = 0;
        this.pc = 0;
    }

    public int getAcc() {
        return acc;
    }

    public int getPc() {
        return pc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public Memory getMemory() {
        return memory;
    }
}