package process;

import java.util.LinkedList;
import java.util.Queue;

public class ProcessQueue {

    private Queue<PCB> processes;

    public ProcessQueue() {
        processes = new LinkedList<>();
    }

    public void add(PCB process) {
        processes.add(process);
    }

    public PCB remove() {
        return processes.poll();
    }

    public PCB peek() {
        return processes.peek();
    }

    public boolean isEmpty() {
        return processes.isEmpty();
    }

    public int size() {
        return processes.size();
    }
}