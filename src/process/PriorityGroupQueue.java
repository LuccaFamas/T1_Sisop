package process;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

// Fila 1 do MLFQ: um grupo FIFO por prioridade (1 a 5).
// Sai primeiro o grupo de maior prioridade; dentro do grupo, FIFO.
public class PriorityGroupQueue {

    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 5;

    // groups.get(p) é o grupo da prioridade p (índice 0 não é usado)
    private final List<ArrayDeque<PCB>> groups;

    public PriorityGroupQueue() {

        groups = new ArrayList<>();

        for (int p = 0; p <= MAX_PRIORITY; p++) {
            groups.add(new ArrayDeque<>());
        }
    }

    // Chegada por rebaixamento ou estouro de quantum: final do grupo
    public void addLastOfGroup(PCB pcb) {
        groupOf(pcb).addLast(pcb);
    }

    // Preempção por evento da Fila 0: topo do grupo
    public void addFirstOfGroup(PCB pcb) {
        groupOf(pcb).addFirst(pcb);
    }

    // Primeiro do grupo não vazio de maior prioridade; null se vazia
    public PCB poll() {

        for (int p = MAX_PRIORITY; p >= MIN_PRIORITY; p--) {

            if (!groups.get(p).isEmpty()) {
                return groups.get(p).pollFirst();
            }
        }

        return null;
    }

    public boolean isEmpty() {

        for (int p = MAX_PRIORITY; p >= MIN_PRIORITY; p--) {

            if (!groups.get(p).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // Na ordem em que sairiam: prioridade 5 -> 1, FIFO dentro do grupo
    public List<PCB> toList() {

        List<PCB> list = new ArrayList<>();

        for (int p = MAX_PRIORITY; p >= MIN_PRIORITY; p--) {
            list.addAll(groups.get(p));
        }

        return list;
    }

    private ArrayDeque<PCB> groupOf(PCB pcb) {

        int priority = pcb.getPriority();

        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                "Prioridade fora de 1-5: " + pcb.getName() + " = " + priority
            );
        }

        return groups.get(priority);
    }
}
