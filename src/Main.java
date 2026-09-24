import java.nio.file.Paths;
import java.util.List;

import process.PCB;
import simulation.ConfigException;
import simulation.ConfigLoader;
import simulation.LivePanel;
import simulation.Monitor;
import simulation.Scheduler;

// Uso: java Main [arquivo_de_configuracao] [passo | milissegundos]
//   sem 2º argumento : tabela com todos os ticks
//   passo            : painel redesenhado a cada tick, avança com Enter
//   500              : painel que avança sozinho a cada 500 ms
public class Main {

    private static final String DEFAULT_CONFIG = "configs/cenario_c.txt";

    public static void main(String[] args) {

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;

        LivePanel livePanel = null;

        if (args.length > 1) {

            if (args[1].equalsIgnoreCase("passo")) {
                livePanel = new LivePanel(true, 0);
            } else {
                int delayMillis;

                try {
                    delayMillis = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    delayMillis = -1;
                }

                if (delayMillis < 0) {
                    System.out.println("Modo inválido: " + args[1]
                        + " (use 'passo' ou milissegundos, ex.: 500)");
                    System.exit(1);
                    return;
                }

                livePanel = new LivePanel(false, delayMillis);
            }
        }

        List<PCB> processes;

        try {
            processes = new ConfigLoader().load(Paths.get(configPath));
        } catch (ConfigException e) {
            System.out.println("Erro na configuração: " + e.getMessage());
            System.exit(1);
            return;
        }

        Monitor monitor = new Monitor(processes);
        Scheduler scheduler = new Scheduler(processes, monitor);

        if (livePanel == null) {
            System.out.println("Configuração: " + configPath);
            System.out.println();
            monitor.printProcesses();
            monitor.printHeader();
        } else {
            scheduler.setLivePanel(livePanel);
        }

        if (!scheduler.run()) {
            System.out.println("Limite de " + Scheduler.MAX_TICKS
                + " ticks atingido: simulação interrompida (loop infinito?)");
        }

        monitor.printGantt(scheduler.getHistory());
        monitor.printStatistics();
    }
}
