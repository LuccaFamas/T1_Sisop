# Erro proposital: salto para label que não existe (linha 6)
.code
    LOAD x
inicio:
    SUB #1
    BRPOS inicoi
    SYSCALL 0
.endcode
.data
    x 5
.enddata
