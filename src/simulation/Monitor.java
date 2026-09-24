package simulation;

import java.util.List;

// Fase 4: só a tabela por tick, no formato dos gabaritos.
// Estados por processo, Gantt e estatísticas entram na Fase 5.
public class Monitor {

    private static final String ROW = "%-4s %-4s %-24s %-10s %-10s %-16s %s";

    public void printHeader() {
        System.out.println(String.format(ROW,
            "t", "CPU", "Instrução executada", "Fila 0", "Fila 1", "Bloqueados", "Evento"));
    }

    public void printTick(TickSnapshot s) {

        String cpu = s.getRunning() == null ? "--" : s.getRunning();

        String instruction = s.getInstruction() == null
            ? "-"
            : s.getInstruction() + " (acc=" + s.getAcc() + ")";

        System.out.println(String.format(ROW,
            s.getTime(),
            cpu,
            instruction,
            join(s.getQueue0()),
            join(s.getQueue1()),
            join(s.getBlocked()),
            s.getEvent()).trim());
    }

    private String join(List<String> items) {
        return items.isEmpty() ? "-" : String.join(",", items);
    }
}
