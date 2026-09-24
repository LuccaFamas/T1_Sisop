# Caso 1: SYSCALL 2, BRNEG, BRZERO, BRANY, MULT e DIV com negativos
.code
        SYSCALL 2        # acc = n, lido do teclado (entrada: -7)
        STORE n
        BRNEG negativo   # -7 < 0: salta
        SYSCALL 0        # não executa
negativo:
        MULT #-3         # -7 * -3 = 21
        DIV #-2          # 21 / -2 = -10 (trunca em direção a zero)
        SYSCALL 1        # imprime -10
        LOAD n
        DIV #2           # -7 / 2 = -3
        SYSCALL 1        # imprime -3
laco:   ADD #1           # -3 -> -2 -> -1 -> 0
        BRZERO fim       # só salta quando acc = 0
        BRANY laco
fim:    SYSCALL 1        # imprime 0
        SYSCALL 0
.endcode
.data
        n 0
.enddata
