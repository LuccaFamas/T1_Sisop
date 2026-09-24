package tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import assembly.AssemblyParseException;
import assembly.AssemblyParser;
import assembly.Program;
import process.PCB;
import process.PriorityGroupQueue;

// Teste da Fase 3. Rodar com: java -cp <saída> tests.QueueTest
public class QueueTest {

    private static int failures = 0;

    public static void main(String[] args) throws AssemblyParseException {

        // O programa não importa aqui; só nome e prioridade
        Program program = new AssemblyParser().parse(
            Arrays.asList(".code", "SYSCALL 0", ".endcode"), "vazio");

        PCB a = new PCB("A", 0, 3, program);
        PCB b = new PCB("B", 0, 5, program);
        PCB c = new PCB("C", 0, 3, program);
        PCB d = new PCB("D", 0, 3, program);

        // 1) Maior prioridade primeiro; mesma prioridade -> FIFO
        PriorityGroupQueue queue = new PriorityGroupQueue();
        queue.addLastOfGroup(a);
        queue.addLastOfGroup(b);
        queue.addLastOfGroup(c);

        check("listagem para o monitor", names(queue.toList()), "[B, A, C]");
        check("poll de A(3), B(5), C(3)", drain(queue), "[B, A, C]");
        check("vazia depois de esvaziar", String.valueOf(queue.isEmpty()), "true");
        check("poll em fila vazia", String.valueOf(queue.poll()), "null");

        // 2) Preempção: D volta ao topo do grupo 3, antes de A
        queue.addLastOfGroup(a);
        queue.addLastOfGroup(b);
        queue.addLastOfGroup(c);
        queue.addFirstOfGroup(d);

        check("addFirstOfGroup(D(3))", drain(queue), "[B, D, A, C]");

        // 3) Prioridade inválida é recusada
        PCB bad = new PCB("X", 0, 7, program);
        String result;
        try {
            queue.addLastOfGroup(bad);
            result = "aceitou";
        } catch (IllegalArgumentException e) {
            result = "recusou";
        }
        check("prioridade 7", result, "recusou");

        System.out.println(failures == 0 ? "\nTODOS OK" : "\n" + failures + " FALHA(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static String drain(PriorityGroupQueue queue) {

        List<PCB> out = new ArrayList<>();
        PCB pcb;

        while ((pcb = queue.poll()) != null) {
            out.add(pcb);
        }

        return names(out);
    }

    private static String names(List<PCB> list) {

        List<String> names = new ArrayList<>();

        for (PCB pcb : list) {
            names.add(pcb.getName());
        }

        return names.toString();
    }

    private static void check(String what, String actual, String expected) {

        boolean ok = actual.equals(expected);

        if (!ok) {
            failures++;
        }

        System.out.println((ok ? "OK     " : "FALHOU ") + what
            + ": " + actual + (ok ? "" : " (esperado " + expected + ")"));
    }
}
