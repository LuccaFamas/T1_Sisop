package assembly;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Resultado do parse de um arquivo .asm. Imutável: vários processos
// podem compartilhar o mesmo Program, cada um com sua própria memória.
public class Program {

    private final String sourceName;
    private final List<Instruction> instructions;
    private final Map<String, Integer> labels;
    private final Map<String, Integer> data;

    public Program(
            String sourceName,
            List<Instruction> instructions,
            Map<String, Integer> labels,
            Map<String, Integer> data) {

        this.sourceName = sourceName;
        this.instructions = Collections.unmodifiableList(instructions);
        this.labels = Collections.unmodifiableMap(labels);
        this.data = Collections.unmodifiableMap(data);
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    // label (minúsculo) -> índice da instrução
    public Map<String, Integer> getLabels() {
        return labels;
    }

    // variável (minúsculo) -> valor inicial
    public Map<String, Integer> getData() {
        return data;
    }
}
