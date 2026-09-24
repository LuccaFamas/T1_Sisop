# Caso 2: processo só de CPU (8 instruções)
.code
        LOAD #3
laco:   SUB #1
        BRPOS laco       # acc: 2 (salta), 1 (salta), 0 (segue)
        SYSCALL 0
.endcode
