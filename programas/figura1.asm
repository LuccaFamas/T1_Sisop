.code
        LOAD variable
ponto1: SUB #1          # label na mesma linha da instrução
        SYSCALL 1
        BRPOS ponto1
        SYSCALL 0
.endcode
.data
    Variable 3
.enddata
