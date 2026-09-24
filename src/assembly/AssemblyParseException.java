package assembly;

public class AssemblyParseException extends Exception {

    private final String sourceName;
    private final int lineNumber;

    public AssemblyParseException(
            String sourceName,
            int lineNumber,
            String message) {

        super(sourceName + ", linha " + lineNumber + ": " + message);

        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
