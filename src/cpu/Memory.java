package cpu;

import java.util.HashMap;
import java.util.Map;

// Memória de dados de UM processo.
public class Memory {

    private final Map<String, Integer> variables;

    // Copia os valores iniciais: alterar esta memória não afeta
    // o Program nem outros processos que usam o mesmo programa.
    public Memory(Map<String, Integer> initialValues) {
        variables = new HashMap<>(initialValues);
    }

    // O parser já garante que toda variável usada foi declarada,
    // então get/set nunca recebem um nome desconhecido.
    public void set(String name, int value) {
        variables.put(name, value);
    }

    public int get(String name) {
        return variables.get(name);
    }
}
