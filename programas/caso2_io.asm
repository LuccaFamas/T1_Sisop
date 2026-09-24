# Caso 2: processo com I/O (4 instruções)
.code
        SYSCALL 1        # imprime 0 e bloqueia
        LOAD #5
        SYSCALL 1        # imprime 5 e bloqueia
        SYSCALL 0
.endcode
