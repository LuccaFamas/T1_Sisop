package assembly;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Lê um arquivo .asm e produz um Program.
// Sem estado de instância: cada chamada de parse usa seus próprios mapas,
// então dois programas nunca misturam labels.
public class AssemblyParser {

    private enum Section {
        OUTSIDE,
        CODE,
        DATA
    }

    // "ponto1: SUB #1" ou "loop:" -> grupo 1 = label, grupo 2 = resto
    private static final Pattern LABEL_PREFIX =
        Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*:(.*)$");

    private static final Pattern IDENTIFIER =
        Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final Pattern INTEGER =
        Pattern.compile("-?[0-9]+");

    // BOM do UTF-8 (EF BB BF) lido como ISO-8859-1
    private static final String UTF8_BOM_AS_LATIN1 = "ï»¿";

    public Program parseFile(Path path)
            throws IOException, AssemblyParseException {

        // ISO-8859-1 nunca falha na decodificação. Toda a sintaxe é ASCII;
        // acentos só aparecem em comentários, que são descartados.
        // Assim o arquivo pode ter sido salvo em UTF-8 ou ANSI no Windows.
        List<String> lines =
            Files.readAllLines(path, StandardCharsets.ISO_8859_1);

        return parse(lines, path.getFileName().toString());
    }

    public Program parse(List<String> lines, String sourceName)
            throws AssemblyParseException {

        List<Instruction> instructions = new ArrayList<>();
        Map<String, Integer> labels = new LinkedHashMap<>();
        Map<String, Integer> labelLines = new HashMap<>();
        Map<String, Integer> data = new LinkedHashMap<>();
        Map<String, Integer> dataLines = new HashMap<>();

        Section section = Section.OUTSIDE;
        boolean codeSeen = false;
        boolean dataSeen = false;

        for (int i = 0; i < lines.size(); i++) {

            int lineNumber = i + 1;
            String line = lines.get(i);

            if (i == 0 && line.startsWith(UTF8_BOM_AS_LATIN1)) {
                line = line.substring(UTF8_BOM_AS_LATIN1.length());
            }

            line = removeComment(line).trim();

            if (line.isEmpty()) {
                continue;
            }

            // Diretivas de seção
            if (line.startsWith(".")) {

                String directive = line.toLowerCase();

                switch (directive) {

                    case ".code":
                        if (section != Section.OUTSIDE) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                ".code dentro de outra seção");
                        }
                        if (codeSeen) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                "seção .code duplicada");
                        }
                        codeSeen = true;
                        section = Section.CODE;
                        break;

                    case ".endcode":
                        if (section != Section.CODE) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                ".endcode sem .code correspondente");
                        }
                        checkDanglingLabels(labels, labelLines,
                            instructions.size(), sourceName);
                        section = Section.OUTSIDE;
                        break;

                    case ".data":
                        if (section != Section.OUTSIDE) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                ".data dentro de outra seção");
                        }
                        if (dataSeen) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                "seção .data duplicada");
                        }
                        dataSeen = true;
                        section = Section.DATA;
                        break;

                    case ".enddata":
                        if (section != Section.DATA) {
                            throw new AssemblyParseException(sourceName, lineNumber,
                                ".enddata sem .data correspondente");
                        }
                        section = Section.OUTSIDE;
                        break;

                    default:
                        throw new AssemblyParseException(sourceName, lineNumber,
                            "diretiva desconhecida: " + line);
                }

                continue;
            }

            switch (section) {

                case CODE:
                    parseCodeLine(line, lineNumber, sourceName,
                        instructions, labels, labelLines);
                    break;

                case DATA:
                    parseDataLine(line, lineNumber, sourceName,
                        data, dataLines);
                    break;

                default:
                    throw new AssemblyParseException(sourceName, lineNumber,
                        "conteúdo fora de .code/.data: " + line);
            }
        }

        int lastLine = lines.size();

        if (section == Section.CODE) {
            throw new AssemblyParseException(sourceName, lastLine,
                "seção .code não foi fechada com .endcode");
        }

        if (section == Section.DATA) {
            throw new AssemblyParseException(sourceName, lastLine,
                "seção .data não foi fechada com .enddata");
        }

        if (instructions.isEmpty()) {
            throw new AssemblyParseException(sourceName, lastLine,
                "programa sem instruções (falta a seção .code?)");
        }

        // Só aqui: .data vem depois de .code, e um salto pode ir
        // para um label definido mais abaixo.
        checkReferences(instructions, labels, data, sourceName);

        return new Program(sourceName, instructions, labels, data);
    }

    // "#" seguido de espaço ou fim de linha inicia comentário.
    // "#" colado em algo ("#5", "#-3") é imediato e fica na linha.
    private String removeComment(String line) {

        for (int i = 0; i < line.length(); i++) {

            if (line.charAt(i) != '#') {
                continue;
            }

            boolean atEnd = i + 1 == line.length();

            if (atEnd || Character.isWhitespace(line.charAt(i + 1))) {
                return line.substring(0, i);
            }
        }

        return line;
    }

    private void parseCodeLine(
            String line,
            int lineNumber,
            String sourceName,
            List<Instruction> instructions,
            Map<String, Integer> labels,
            Map<String, Integer> labelLines)
            throws AssemblyParseException {

        Matcher labelMatcher = LABEL_PREFIX.matcher(line);

        if (labelMatcher.matches()) {

            String label = labelMatcher.group(1).toLowerCase();

            if (labels.containsKey(label)) {
                throw new AssemblyParseException(sourceName, lineNumber,
                    "label duplicado: " + label
                    + " (já definido na linha " + labelLines.get(label) + ")");
            }

            // Aponta para a próxima instrução a ser adicionada
            labels.put(label, instructions.size());
            labelLines.put(label, lineNumber);

            line = labelMatcher.group(2).trim();

            // Label sozinho na linha
            if (line.isEmpty()) {
                return;
            }
        }

        String[] parts = line.split("\\s+");

        InstructionType type = parseType(parts[0], lineNumber, sourceName);

        if (parts.length < 2) {
            throw new AssemblyParseException(sourceName, lineNumber,
                type + " sem operando");
        }

        if (parts.length > 2) {
            throw new AssemblyParseException(sourceName, lineNumber,
                "texto inesperado após o operando de " + type + ": " + parts[2]);
        }

        String operand = parseOperand(type, parts[1], lineNumber, sourceName);

        instructions.add(new Instruction(type, operand, lineNumber));
    }

    private InstructionType parseType(
            String mnemonic,
            int lineNumber,
            String sourceName)
            throws AssemblyParseException {

        try {
            return InstructionType.valueOf(mnemonic.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AssemblyParseException(sourceName, lineNumber,
                "instrução desconhecida: " + mnemonic);
        }
    }

    private String parseOperand(
            InstructionType type,
            String text,
            int lineNumber,
            String sourceName)
            throws AssemblyParseException {

        switch (type) {

            // Imediato (#n) ou direto (variável)
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case LOAD:
                if (text.startsWith("#")) {
                    int value = parseInteger(text.substring(1),
                        "imediato", lineNumber, sourceName);
                    return "#" + value;
                }
                return parseIdentifier(text, lineNumber, sourceName);

            // Somente direto
            case STORE:
                if (text.startsWith("#")) {
                    throw new AssemblyParseException(sourceName, lineNumber,
                        "STORE aceita somente variável, não imediato: " + text);
                }
                return parseIdentifier(text, lineNumber, sourceName);

            // Label
            case BRANY:
            case BRPOS:
            case BRZERO:
            case BRNEG:
                return parseIdentifier(text, lineNumber, sourceName);

            case SYSCALL:
                if (!text.equals("0") && !text.equals("1") && !text.equals("2")) {
                    throw new AssemblyParseException(sourceName, lineNumber,
                        "SYSCALL aceita somente 0, 1 ou 2: " + text);
                }
                return text;

            default:
                throw new AssemblyParseException(sourceName, lineNumber,
                    "instrução sem regra de operando: " + type);
        }
    }

    private String parseIdentifier(
            String text,
            int lineNumber,
            String sourceName)
            throws AssemblyParseException {

        if (!IDENTIFIER.matcher(text).matches()) {
            throw new AssemblyParseException(sourceName, lineNumber,
                "operando inválido: " + text
                + " (use #n para imediato ou um nome de variável/label)");
        }

        // Identificadores são case-insensitive
        return text.toLowerCase();
    }

    private int parseInteger(
            String text,
            String what,
            int lineNumber,
            String sourceName)
            throws AssemblyParseException {

        if (!INTEGER.matcher(text).matches()) {
            throw new AssemblyParseException(sourceName, lineNumber,
                what + " inválido: " + text);
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new AssemblyParseException(sourceName, lineNumber,
                what + " fora do intervalo de int: " + text);
        }
    }

    private void parseDataLine(
            String line,
            int lineNumber,
            String sourceName,
            Map<String, Integer> data,
            Map<String, Integer> dataLines)
            throws AssemblyParseException {

        String[] parts = line.split("\\s+");

        if (parts.length != 2) {
            throw new AssemblyParseException(sourceName, lineNumber,
                "linha de dados deve ter o formato 'nome valor': " + line);
        }

        String name = parseIdentifier(parts[0], lineNumber, sourceName);

        if (data.containsKey(name)) {
            throw new AssemblyParseException(sourceName, lineNumber,
                "variável duplicada: " + name
                + " (já declarada na linha " + dataLines.get(name) + ")");
        }

        int value = parseInteger(parts[1], "valor", lineNumber, sourceName);

        data.put(name, value);
        dataLines.put(name, lineNumber);
    }

    // Label no fim de .code, sem instrução depois, faria o salto
    // cair fora do programa.
    private void checkDanglingLabels(
            Map<String, Integer> labels,
            Map<String, Integer> labelLines,
            int instructionCount,
            String sourceName)
            throws AssemblyParseException {

        for (Map.Entry<String, Integer> entry : labels.entrySet()) {

            if (entry.getValue() == instructionCount) {
                throw new AssemblyParseException(sourceName,
                    labelLines.get(entry.getKey()),
                    "label " + entry.getKey() + " não é seguido de nenhuma instrução");
            }
        }
    }

    private void checkReferences(
            List<Instruction> instructions,
            Map<String, Integer> labels,
            Map<String, Integer> data,
            String sourceName)
            throws AssemblyParseException {

        for (Instruction instruction : instructions) {

            String operand = instruction.getOperand();

            switch (instruction.getType()) {

                case BRANY:
                case BRPOS:
                case BRZERO:
                case BRNEG:
                    if (!labels.containsKey(operand)) {
                        throw new AssemblyParseException(sourceName,
                            instruction.getLineNumber(),
                            "label não definido: " + operand);
                    }
                    break;

                case SYSCALL:
                    break;

                default:
                    if (!operand.startsWith("#") && !data.containsKey(operand)) {
                        throw new AssemblyParseException(sourceName,
                            instruction.getLineNumber(),
                            "variável não declarada em .data: " + operand);
                    }
                    break;
            }
        }
    }
}
