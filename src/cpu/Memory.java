package cpu;

import java.util.HashMap;
import java.util.Map;

public class Memory {

    private Map<String, Integer> variables;

    public Memory() {
        variables = new HashMap<>();
    }

    public void set(String name, int value) {
        variables.put(name, value);
    }

    public int get(String name) {

        if (!variables.containsKey(name)) {
            throw new IllegalArgumentException(
                "Variável não encontrada: " + name
            );
        }

        return variables.get(name);
    }
}