import java.nio.file.Paths;
import java.util.List;

import process.PCB;
import simulation.ConfigException;
import simulation.ConfigLoader;
import simulation.Monitor;
import simulation.Scheduler;

// Uso: java Main [arquivo_de_configuracao] [milissegundos_por_UT]
// Ex.: java Main configs/cenario_c.txt 500  -> cada UT dura 0,5 s na tela
public class Main {

    private static final String DEFAULT_CONFIG = "configs/cenario_c.txt";

    public static void main(String[] args) {

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;

        int tickDelayMillis = 0;

        if (args.length > 1) {
            try {
                tickDelayMillis = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                tickDelayMillis = -1;
            }

            if (tickDelayMillis < 0) {
                System.out.println("Milissegundos por UT inválido: " + args[1]
                    + " (use um inteiro >= 0, ex.: 500)");
                System.exit(1);
                return;
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

        System.out.println("Configuração: " + configPath);
        System.out.println();

        Monitor monitor = new Monitor(processes);
        Scheduler scheduler = new Scheduler(processes, monitor);

        monitor.printProcesses();
        monitor.printHeader();

        if (!scheduler.run(tickDelayMillis)) {
            System.out.println("Limite de " + Scheduler.MAX_TICKS
                + " ticks atingido: simulação interrompida (loop infinito?)");
        }

        monitor.printGantt(scheduler.getHistory());
        monitor.printStatistics();
    }
}
