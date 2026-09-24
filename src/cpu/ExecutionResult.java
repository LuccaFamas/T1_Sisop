package cpu;

// O que o SO precisa fazer depois da instrução.
public enum ExecutionResult {

    CONTINUE,
    SYSCALL_HALT,
    SYSCALL_PRINT,
    SYSCALL_READ
}
