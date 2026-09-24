.code
    LOAD valor
    ADD #5          # acc = 10 + 5 = 15
    STORE valor
    SYSCALL 1       # imprime 15 e bloqueia 3 UTs
    SYSCALL 0       # halt
.endcode
.data
    valor 10
.enddata
