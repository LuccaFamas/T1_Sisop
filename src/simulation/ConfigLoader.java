package simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import assembly.AssemblyParseException;
import assembly.AssemblyParser;
import assembly.Program;
import process.PCB;
import process.PriorityGroupQueue;

// Lê o arquivo de configuração: um processo por linha,
//   nome chegada prioridade caminho.asm
// Linhas vazias e iniciadas por # são ignoradas.
// O caminho do .asm é relativo à pasta de onde o simulador é executado
// (os scripts build.bat/build.sh entram na raiz do projeto).
public class ConfigLoader {

    // BOM do UTF-8 (EF BB BF) lido como ISO-8859-1
    private static final String UTF8_BOM_AS_LATIN1 = "ï»¿";

    public List<PCB> load(Path configPath) throws ConfigException {

        List<String> lines;

        try {
            lines = Files.readAllLines(configPath, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new ConfigException("não foi possível ler " + configPath);
        }

        String source = configPath.getFileName().toString();

        AssemblyParser parser = new AssemblyParser();
        List<PCB> processes = new ArrayList<>();
        Map<String, Integer> nameLines = new HashMap<>();

        for (int i = 0; i < lines.size(); i++) {

            int lineNumber = i + 1;
            String line = lines.get(i);

            // Bloco de Notas pode salvar "UTF-8 com BOM"
            if (i == 0 && line.startsWith(UTF8_BOM_AS_LATIN1)) {
                line = line.substring(UTF8_BOM_AS_LATIN1.length());
            }

            line = line.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String where = source + ", linha " + lineNumber + ": ";

            // Limite 4: o caminho pode conter espaços
            String[] parts = line.split("\\s+", 4);

            if (parts.length < 4) {
                throw new ConfigException(where
                    + "formato esperado 'nome chegada prioridade caminho.asm': " + line);
            }

            String name = parts[0];

            if (nameLines.containsKey(name)) {
                throw new ConfigException(where + "processo " + name
                    + " repetido (já definido na linha " + nameLines.get(name) + ")");
            }

            int arrival = parseInt(parts[1], "chegada", where);

            if (arrival < 0) {
                throw new ConfigException(where + "chegada negativa: " + arrival);
            }

            int priority = parseInt(parts[2], "prioridade", where);

            if (priority < PriorityGroupQueue.MIN_PRIORITY
                    || priority > PriorityGroupQueue.MAX_PRIORITY) {
                throw new ConfigException(where + "prioridade deve ser de "
                    + PriorityGroupQueue.MIN_PRIORITY + " a "
                    + PriorityGroupQueue.MAX_PRIORITY + ": " + priority);
            }

            Path programPath = Paths.get(parts[3].trim());

            if (!Files.isRegularFile(programPath)) {
                throw new ConfigException(where + "arquivo não encontrado: "
                    + programPath.toAbsolutePath());
            }

            Program program;

            try {
                program = parser.parseFile(programPath);
            } catch (IOException e) {
                throw new ConfigException(where + "não foi possível ler " + programPath);
            } catch (AssemblyParseException e) {
                throw new ConfigException(where + "erro no programa de " + name
                    + " -> " + e.getMessage());
            }

            processes.add(new PCB(name, arrival, priority, program));
            nameLines.put(name, lineNumber);
        }

        if (processes.isEmpty()) {
            throw new ConfigException(source + ": nenhum processo definido");
        }

        return processes;
    }

    private int parseInt(String text, String what, String where)
            throws ConfigException {

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ConfigException(where + what + " não é um inteiro: " + text);
        }
    }
}
