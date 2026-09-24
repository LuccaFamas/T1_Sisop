import java.nio.file.Paths;
import java.util.List;

import process.PCB;
import simulation.ConfigException;
import simulation.ConfigLoader;
import simulation.Monitor;
import simulation.Scheduler;

// Uso: java Main [arquivo_de_configuracao]
public class Main {

    private static final String DEFAULT_CONFIG = "configs/cenario_c.txt";

    public static void main(String[] args) {

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;

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

        if (!scheduler.run()) {
            System.out.println("Limite de " + Scheduler.MAX_TICKS
                + " ticks atingido: simulação interrompida (loop infinito?)");
        }

        monitor.printGantt(scheduler.getHistory());
        monitor.printStatistics();
    }
}
