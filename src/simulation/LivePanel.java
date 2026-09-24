package simulation;

import java.io.IOException;
import java.util.List;

import process.PCB;

// Modo de apresentação: limpa a tela e redesenha o estado a cada tick,
// avançando com Enter (passo a passo) ou sozinho após uma pausa.
// Só muda a exibição; a simulação é exatamente a mesma do modo tabela.
public class LivePanel {

    // Últimas mensagens de syscall mostradas no painel
    private static final int LOG_LINES = 6;

    // true = espera Enter; false = espera delayMillis
    private final boolean waitForEnter;
    private final int delayMillis;

    public LivePanel(boolean waitForEnter, int delayMillis) {
        this.waitForEnter = waitForEnter;
        this.delayMillis = delayMillis;
    }

    public void show(
            TickSnapshot s,
            List<TickSnapshot> history,
            List<PCB> processes,
            List<String> log) {

        clearScreen();

        System.out.println("===== t = " + s.getTime() + " =====");
        System.out.println();

        String cpu = s.getRunning() == null
            ? "-- (ociosa)"
            : s.getRunning() + "  " + s.getInstruction() + " (acc=" + s.getAcc() + ")";

        System.out.println("CPU        : " + cpu);
        System.out.println("Evento     : " + (s.getEvent().isEmpty() ? "-" : s.getEvent()));
        System.out.println("Fila 0     : " + join(s.getQueue0()));
        System.out.println("Fila 1     : " + join(s.getQueue1()));
        System.out.println("Bloqueados : " + join(s.getBlocked()));
        System.out.println();

        for (PCB pcb : processes) {
            System.out.println(String.format("%-5s %-11s pc=%-3d acc=%d",
                pcb.getName(),
                Monitor.stateName(s.getStates().get(pcb.getName())),
                pcb.getPc(),
                pcb.getAcc()));
        }

        System.out.println();

        StringBuilder gantt = new StringBuilder("Gantt: ");

        for (TickSnapshot past : history) {
            gantt.append(past.getRunning() == null ? "--" : past.getRunning()).append(' ');
        }

        System.out.println(gantt.toString().trim());
        System.out.println();

        System.out.println("Saída dos programas:");

        for (int i = Math.max(0, log.size() - LOG_LINES); i < log.size(); i++) {
            System.out.println("  " + log.get(i));
        }

        System.out.println();

        waitNext();
    }

    private void waitNext() {

        if (waitForEnter) {
            System.out.print("(Enter = próximo tick)");

            // Sem entrada (arquivo acabou): segue sem esperar
            if (OperatingSystem.KEYBOARD.hasNextLine()) {
                OperatingSystem.KEYBOARD.nextLine();
            }
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // No Windows, "cls" funciona em qualquer console (cmd, PowerShell).
    // No Linux/macOS, o código ANSI de limpar a tela.
    private void clearScreen() {

        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException e) {
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String join(List<String> items) {
        return items.isEmpty() ? "-" : String.join("  ", items);
    }
}
