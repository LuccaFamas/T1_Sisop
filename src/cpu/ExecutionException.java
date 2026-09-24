package cpu;

// Erro em tempo de execução de um processo (divisão por zero,
// PC fora do programa). Encerra só aquele processo.
public class ExecutionException extends Exception {

    public ExecutionException(String message) {
        super(message);
    }
}
