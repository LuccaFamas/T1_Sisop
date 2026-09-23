package assembly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyParser {

    private Map<String, Integer> labels;

    public AssemblyParser() {
        labels = new HashMap<>();
    }

    public List<Instruction> parseCode(List<String> lines) {

        List<Instruction> instructions = new ArrayList<>();

        for (String line : lines) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equals(".code") ||
                line.equals(".endcode")) {
                continue;
            }

            // Exemplo:
            // loop:
            if (line.endsWith(":")) {

                String label =
                    line.substring(0, line.length() - 1);

                labels.put(label, instructions.size());

                continue;
            }

            String[] parts = line.split("\\s+");

            String operation = parts[0];

            String operand = null;

            if (parts.length > 1) {
                operand = parts[1];
            }

            instructions.add(
                new Instruction(operation, operand)
            );
        }

        return instructions;
    }

    public Map<String, Integer> getLabels() {
        return labels;
    }
}