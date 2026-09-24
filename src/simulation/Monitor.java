package simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import assembly.Instruction;
import process.PCB;
import process.ProcessState;

// Toda a saída da simulação: tabela por tick, Gantt e estatísticas.
// A tabela é impressa tick a tick (e não no fim), para que as mensagens
// de SYSCALL e a leitura do teclado apareçam no momento certo.
public class Monitor {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    // Ticks por linha no Gantt, para não estourar a largura do terminal
    private static final int GANTT_TICKS_PER_LINE = 20;

    private final List<PCB> processes;

    // Larguras das colunas, calculadas a partir dos processos
    private final int nameWidth;
    private final int stateWidth;
    private final int instructionWidth;
    private final int queue0Width;
    private final int queue1Width;
    private final int blockedWidth;

    public Monitor(List<PCB> processes) {

        this.processes = new ArrayList<>(processes);
        this.processes.sort(Comparator.comparing(PCB::getName));

        int maxName = 3;
        int maxInstruction = 0;

        for (PCB pcb : this.processes) {

            maxName = Math.max(maxName, pcb.getName().length());

            for (Instruction instruction : pcb.getProgram().getInstructions()) {
                maxInstruction = Math.max(maxInstruction, instruction.toString().length());
            }
        }

        int n = this.processes.size();

        this.nameWidth = maxName;
        this.stateWidth = Math.max(maxName, "Executando".length());
        // " (acc=-999)" cobre os casos comuns; valores maiores só empurram a linha
        this.instructionWidth = Math.max("Instrução".length(), maxInstruction + 11);
        this.queue0Width = Math.max("Fila 0".length(), n * (maxName + 1) - 1);
        this.queue1Width = Math.max("Fila 1".length(), n * (maxName + 4) - 1);
        this.blockedWidth = Math.max("Bloqueados".length(), n * (maxName + 5) - 1);
    }

    public void printProcesses() {

        System.out.println("Processos:");

        for (PCB pcb : processes) {
            System.out.println("  " + pad(pcb.getName(), nameWidth)
                + "  chegada " + pcb.getArrivalTime()
                + "  prioridade " + pcb.getPriority()
                + "  programa " + pcb.getProgram().getSourceName());
        }

        System.out.println();
        System.out.println("Estados: - = ainda não chegou.  Fila 1: nome(prioridade).  "
            + "Bloqueados: nome->tick em que volta a Pronto.");
        System.out.println("Filas e estados são o retrato do tick, antes do evento da última coluna.");
        System.out.println();
    }

    public void printHeader() {

        StringBuilder line = new StringBuilder();

        line.append(pad("t", 4)).append(" ");
        line.append(pad("CPU", nameWidth)).append("  ");
        line.append(pad("Instrução", instructionWidth)).append("  ");

        for (PCB pcb : processes) {
            line.append(pad(pcb.getName(), stateWidth)).append("  ");
        }

        line.append(pad("Fila 0", queue0Width)).append("  ");
        line.append(pad("Fila 1", queue1Width)).append("  ");
        line.append(pad("Bloqueados", blockedWidth)).append("  ");
        line.append("Evento");

        System.out.println(rtrim(line.toString()));
        System.out.println(repeat('-', line.length()));
    }

    public void printTick(TickSnapshot s) {

        StringBuilder line = new StringBuilder();

        line.append(pad(String.valueOf(s.getTime()), 4)).append(" ");
        line.append(pad(s.getRunning() == null ? "--" : s.getRunning(), nameWidth)).append("  ");

        String instruction = s.getInstruction() == null
            ? "-"
            : s.getInstruction() + " (acc=" + s.getAcc() + ")";

        line.append(pad(instruction, instructionWidth)).append("  ");

        Map<String, ProcessState> states = s.getStates();

        for (PCB pcb : processes) {
            line.append(pad(stateName(states.get(pcb.getName())), stateWidth)).append("  ");
        }

        line.append(pad(join(s.getQueue0()), queue0Width)).append("  ");
        line.append(pad(join(s.getQueue1()), queue1Width)).append("  ");
        line.append(pad(join(s.getBlocked()), blockedWidth)).append("  ");
        line.append(s.getEvent());

        System.out.println(rtrim(line.toString()));
    }

    public void printGantt(List<TickSnapshot> history) {

        System.out.println();
        System.out.println("Diagrama de Gantt (-- = CPU ociosa):");

        int cell = Math.max(nameWidth, String.valueOf(history.size()).length()) + 1;

        for (int start = 0; start < history.size(); start += GANTT_TICKS_PER_LINE) {

            int end = Math.min(start + GANTT_TICKS_PER_LINE, history.size());

            StringBuilder ticks = new StringBuilder("  t  : ");
            StringBuilder cpu = new StringBuilder("  CPU: ");

            for (int i = start; i < end; i++) {

                TickSnapshot s = history.get(i);

                ticks.append(pad(String.valueOf(s.getTime()), cell));
                cpu.append(pad(s.getRunning() == null ? "--" : s.getRunning(), cell));
            }

            System.out.println(rtrim(ticks.toString()));
            System.out.println(rtrim(cpu.toString()));
            System.out.println();
        }
    }

    public void printStatistics() {

        System.out.println("Estatísticas (em UTs):");

        String row = "  %-" + Math.max(nameWidth, 8) + "s  %7s  %7s  %10s  %4s  %4s  %15s  %16s  %s";

        System.out.println(rtrim(String.format(row,
            "Processo", "Chegada", "Término", "Turnaround", "CPU", "I/O", "Espera (direta)", "TA-CPU-I/O", "")));

        int finishedCount = 0;
        int turnaroundSum = 0;
        int waitSum = 0;

        for (PCB pcb : processes) {

            if (pcb.getState() != ProcessState.FINISHED) {
                System.out.println(rtrim(String.format(row,
                    pcb.getName(), pcb.getArrivalTime(), "-", "-",
                    pcb.getCpuTime(), pcb.getIoTime(), pcb.getWaitTime(), "-",
                    "não terminou")));
                continue;
            }

            int turnaround = pcb.getFinishTime() - pcb.getArrivalTime();

            // Verificação cruzada: todo tick entre chegada e término o
            // processo esteve executando, bloqueado ou pronto
            int crossCheck = turnaround - pcb.getCpuTime() - pcb.getIoTime();

            String note = pcb.hasEndedWithError() ? "(erro)" : "";

            if (crossCheck != pcb.getWaitTime()) {
                note = (note + " ATENÇÃO: espera direta difere da cruzada").trim();
            }

            System.out.println(rtrim(String.format(row,
                pcb.getName(), pcb.getArrivalTime(), pcb.getFinishTime(), turnaround,
                pcb.getCpuTime(), pcb.getIoTime(), pcb.getWaitTime(), crossCheck,
                note)));

            finishedCount++;
            turnaroundSum += turnaround;
            waitSum += pcb.getWaitTime();
        }

        System.out.println();

        if (finishedCount == 0) {
            System.out.println("Nenhum processo terminou: sem médias.");
            return;
        }

        System.out.println(String.format(PT_BR,
            "Turnaround médio: %.2f   Tempo médio de espera (fila de prontos): %.2f",
            (double) turnaroundSum / finishedCount,
            (double) waitSum / finishedCount));
    }

    private String stateName(ProcessState state) {

        switch (state) {
            case READY:
                return "Pronto";
            case RUNNING:
                return "Executando";
            case BLOCKED:
                return "Bloqueado";
            case FINISHED:
                return "Finalizado";
            default:
                return "-";
        }
    }

    private String join(List<String> items) {
        return items.isEmpty() ? "-" : String.join(",", items);
    }

    private String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    // Remove espaços só do fim (trim() tiraria também o recuo)
    private String rtrim(String text) {

        int end = text.length();

        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }

        return text.substring(0, end);
    }

    private String repeat(char c, int count) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            sb.append(c);
        }

        return sb.toString();
    }
}
