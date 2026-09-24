#!/bin/sh
# Compila do zero e roda o simulador.
# Uso: ./build.sh [arquivo_de_configuracao]   (padrão: configs/cenario_c.txt)

cd "$(dirname "$0")" || exit 1

rm -rf bin
mkdir bin

# -sourcepath: o javac acha sozinho todas as classes usadas a partir do Main
javac -encoding UTF-8 -d bin -sourcepath src src/Main.java src/tests/QueueTest.java || {
    echo "Erro de compilacao."
    exit 1
}

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin Main "$@"
