@echo off
rem Compila do zero e roda o simulador.
rem Uso: build.bat [arquivo_de_configuracao]   (padrao: configs\cenario_c.txt)

cd /d "%~dp0"

if exist bin rmdir /s /q bin
mkdir bin

rem -sourcepath: o javac acha sozinho todas as classes usadas a partir do Main
javac -encoding UTF-8 -d bin -sourcepath src src\Main.java src\tests\QueueTest.java
if errorlevel 1 (
    echo Erro de compilacao.
    exit /b 1
)

rem Console em UTF-8 para os acentos aparecerem certos
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin Main %*
