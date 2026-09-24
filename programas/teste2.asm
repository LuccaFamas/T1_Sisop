.code
    LOAD limite
loop:
    SUB #1          # acc = acc - 1
    STORE temp
    SYSCALL 1       # imprime acc e bloqueia 3 UTs
    LOAD temp
    BRPOS loop
    SYSCALL 0       # halt
.endcode
.data
    limite 3
    temp 0
.enddata
