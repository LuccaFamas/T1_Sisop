package process;

import java.util.LinkedList;
import java.util.Queue;

public class ProcessQueue {

    private Queue<Process> processes;

    public ProcessQueue() {
        processes = new LinkedList<>();
    }

    public void add(Process process) {
        processes.add(process);
    }

    public Process remove() {
        return processes.poll();
    }

    public Process peek() {
        return processes.peek();
    }

    public boolean isEmpty() {
        return processes.isEmpty();
    }

    public int size() {
        return processes.size();
    }
}