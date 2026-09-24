package simulation;

import java.util.List;
import java.util.Map;

import process.ProcessState;

// Registro de um tick para o monitor e o Gantt.
// Filas e estados são o retrato de DEPOIS do dispatch e ANTES do
// pós-execução; instrução, acc e evento são preenchidos no fim do tick.
public class TickSnapshot {

    private final int time;

    // null = CPU ociosa
    private final String running;

    private final List<String> queue0;
    private final List<String> queue1;
    private final List<String> blocked;

    // nome -> estado, em ordem de nome
    private final Map<String, ProcessState> states;

    private String instruction;
    private int acc;
    private String event;

    public TickSnapshot(
            int time,
            String running,
            List<String> queue0,
            List<String> queue1,
            List<String> blocked,
            Map<String, ProcessState> states) {

        this.time = time;
        this.running = running;
        this.queue0 = queue0;
        this.queue1 = queue1;
        this.blocked = blocked;
        this.states = states;
        this.instruction = null;
        this.event = "";
    }

    public int getTime() {
        return time;
    }

    public String getRunning() {
        return running;
    }

    public List<String> getQueue0() {
        return queue0;
    }

    public List<String> getQueue1() {
        return queue1;
    }

    public List<String> getBlocked() {
        return blocked;
    }

    public Map<String, ProcessState> getStates() {
        return states;
    }

    public String getInstruction() {
        return instruction;
    }

    public int getAcc() {
        return acc;
    }

    public String getEvent() {
        return event;
    }

    public void setExecution(String instruction, int acc) {
        this.instruction = instruction;
        this.acc = acc;
    }

    public void addEvent(String text) {
        event = event.isEmpty() ? text : event + "; " + text;
    }
}
