# TP1 Sisop — Simulador de Execução Dinâmica de Processos (MLFQ)

Simulador de uma máquina baseada em acumulador que executa vários programas
em assembly simplificado, escalonados por um MLFQ de duas filas. A cada
unidade de tempo (UT) mostra o estado dos processos e das filas; no fim,
o diagrama de Gantt e as estatísticas.

## Requisitos

- JDK 8 ou superior (testado com JDK 8, 21 e 23). Sem bibliotecas externas.
- Windows (cmd ou PowerShell) ou Linux.

## Como compilar e rodar

O script compila tudo do zero em `bin/` e já executa a simulação.

| Ambiente   | Comando                                |
|------------|----------------------------------------|
| cmd        | `build.bat configs\cenario_c.txt`      |
| PowerShell | `.\build.bat configs\cenario_c.txt`    |
| Linux      | `./build.sh configs/cenario_c.txt`     |

Sem argumento, usa `configs/cenario_c.txt` (exemplo do enunciado).

### Modo de apresentação (painel)

Um segundo argumento troca a tabela por um painel que limpa a tela e
mostra um tick por vez: o que está na CPU, as filas, os bloqueados, o
estado de cada processo, o Gantt até ali e as mensagens das syscalls.
Só muda a exibição; o resultado é o mesmo.

```
build.bat configs\cenario_c.txt passo    (avança um tick a cada Enter)
build.bat configs\cenario_c.txt 800      (avança sozinho a cada 800 ms)
./build.sh configs/cenario_c.txt passo
```

Ao fim, mostra o Gantt completo e as estatísticas, como no modo tabela.

Programas com `SYSCALL 2` pedem um inteiro pelo teclado. Para testes
reproduzíveis, a entrada pode vir de um arquivo:

```
build.bat configs\caso1.txt < configs\caso1_entrada.txt
./build.sh configs/caso1.txt < configs/caso1_entrada.txt
```

Teste das filas (Fase 3), depois de compilar:

```
java -cp bin tests.QueueTest
```

## Estrutura

```
src/
  Main.java                  carrega a configuração e roda a simulação
  assembly/                  parser do .asm -> Program (instruções, labels, dados)
  cpu/                       InstructionExecutor (1 instrução por chamada) e Memory
  process/                   PCB e Fila 1 (PriorityGroupQueue)
  simulation/                Scheduler (laço de ticks), OperatingSystem (syscalls),
                             Monitor (saída), ConfigLoader
  tests/QueueTest.java       teste da Fila 1
programas/                   programas .asm
configs/                     arquivos de configuração
```

## Arquivo de configuração

Um processo por linha: `nome chegada prioridade caminho.asm`.
Linhas vazias e iniciadas por `#` são ignoradas.

```
# nome  chegada  prioridade  programa
P01     0        3           programas/teste1.asm
P02     1        5           programas/teste2.asm
```

- Prioridade de 1 a 5 (maior = mais prioritário). Chegada ≥ 0.
- O caminho do `.asm` é relativo à pasta do projeto (os scripts entram nela).
- Use nomes com o mesmo número de dígitos (`P01`, `P02`, ..., `P10`): o
  desempate entre processos é por ordem alfabética de nome, e assim ela
  coincide com a numérica (`P10` viria antes de `P2`).
- Erros (formato, nome repetido, prioridade fora de 1–5, arquivo
  inexistente, erro de sintaxe no programa) são informados com o número
  da linha, e a simulação não começa.

## Formato dos programas

```
.code
        LOAD variable
ponto1: SUB #1          # comentário
        SYSCALL 1
        BRPOS ponto1
        SYSCALL 0
.endcode
.data
    variable 3
.enddata
```

| Instrução      | Efeito                              | Operando              |
|----------------|-------------------------------------|-----------------------|
| `ADD op`       | acc = acc + op                      | `#n` ou variável      |
| `SUB op`       | acc = acc − op                      | `#n` ou variável      |
| `MULT op`      | acc = acc × op                      | `#n` ou variável      |
| `DIV op`       | acc = acc ÷ op                      | `#n` ou variável      |
| `LOAD op`      | acc = op                            | `#n` ou variável      |
| `STORE op`     | op = acc                            | somente variável      |
| `BRANY label`  | pc ← label                          |                       |
| `BRPOS label`  | se acc > 0, pc ← label              |                       |
| `BRZERO label` | se acc = 0, pc ← label              |                       |
| `BRNEG label`  | se acc < 0, pc ← label              |                       |
| `SYSCALL n`    | 0 = fim, 1 = imprime acc, 2 = lê inteiro para acc |         |

- Label sozinho na linha (`loop:`) ou antes da instrução (`ponto1: SUB #1`).
- `#` colado em número (`#5`, `#-3`) é imediato; `#` seguido de espaço ou
  fim de linha inicia comentário.
- Mnemônicos, variáveis e labels não diferenciam maiúsculas de minúsculas.
- Erros são detectados na leitura, com arquivo e linha: instrução
  desconhecida, operando ausente ou inválido, `STORE #n`, `SYSCALL` diferente
  de 0/1/2, label ou variável inexistente, label duplicado, seção não fechada.

## Regras de escalonamento

- **Fila 0** (maior prioridade): Round Robin FIFO, quantum 2. Todo processo
  novo e todo processo que volta de I/O entra no final dela. Se esgota o
  quantum, é rebaixado para a Fila 1.
- **Fila 1**: Round Robin por prioridade estática (5 → 1), quantum 4;
  mesma prioridade → FIFO. Estouro de quantum → final do seu grupo.
- A Fila 1 só usa a CPU se a Fila 0 está vazia. Um processo da Fila 1 em
  execução é interrompido assim que alguém entra na Fila 0; volta ao
  **topo** do seu grupo, mantendo o quantum restante.
- `SYSCALL 1` e `SYSCALL 2` bloqueiam o processo por 3 UTs.

### Ordem dos eventos em cada tick t

1. **Desbloqueio**: quem volta de I/O em t vai para o final da Fila 0.
2. **Admissão**: quem chega em t vai para o final da Fila 0.
3. **Preempção**: se quem está na CPU é da Fila 1 e a Fila 0 não está
   vazia, ele volta ao topo do seu grupo.
4. **Dispatch**: se a CPU está livre, pega da Fila 0; senão, da Fila 1;
   senão, o tick fica ocioso (`--`).
5. **Snapshot** do estado para o monitor.
6. **Execução** de 1 instrução.
7. **Pós-execução**, nesta precedência: fim do processo → bloqueio por I/O
   → estouro de quantum.

## Decisões de interpretação

Pontos que o enunciado não fixa e como o simulador os trata:

| Situação | Decisão |
|---|---|
| Relógio | O tick t é o intervalo [t, t+1); no máximo 1 instrução por tick. Troca de contexto custa 0. |
| Bloqueio | SYSCALL 1/2 executa em t (gasta 1 UT de CPU), o processo fica bloqueado em t+1, t+2 e t+3 e volta a Pronto em t+4. |
| Impressão e leitura | Acontecem no tick em que a SYSCALL executa, não no desbloqueio. A leitura repete até receber um inteiro válido. |
| Término | t+1, onde t é o tick do `SYSCALL 0`. Turnaround = término − chegada. |
| Espera | Ticks em que o processo esteve Pronto (em qualquer fila, fora da CPU). É contada diretamente e conferida com TA − CPU − I/O. |
| Empates no mesmo tick | Desbloqueios entram na Fila 0 antes das chegadas; entre iguais, ordem alfabética de nome. |
| Fila 0 × Fila 0 | Uma chegada na Fila 0 não interrompe outro processo da Fila 0. |
| SYSCALL no último tick do quantum | O processo bloqueia e **não** é rebaixado (I/O tem precedência sobre estouro de quantum). |
| Erro de execução | Divisão por zero, PC fora do programa ou fim da entrada durante `SYSCALL 2` encerram só aquele processo, com a mensagem `[Pxx] Erro de execução: ...`. Ele é tratado como um fim normal: a instrução conta 1 UT de CPU, término t+1, entra nas médias e aparece marcado "(erro)". |
| `STORE` em variável não declarada | Erro de sintaxe. Toda variável deve estar em `.data` (o próprio `teste2.asm` declara `temp 0` mesmo sem ler `temp` antes de gravá-la). |
| Aritmética | Inteiros de 32 bits. A divisão trunca em direção a zero (−7 ÷ 2 = −3); `MULT` pode estourar e "dar a volta". |
| Processo que ainda não chegou | Aparece como `-` na coluna de estados. |
| Parada | Quando todos os processos terminam, ou no limite de segurança de 10.000 ticks (contra loop infinito). |
| Figura 1 | O enunciado declara `Variable` e usa `variable`; como identificadores não diferenciam maiúsculas, o programa funciona. |

## Saída

A cada tick, uma linha com a instrução executada e o valor de acc, o
estado de cada processo, as filas e os bloqueados (`nome->tick em que volta`),
e o evento do tick. As mensagens de syscall aparecem no tick em que ocorrem:

```
[P01] Impressão (SYSCALL 1): 15
[P01] Finalizado (SYSCALL 0)
```

No fim: diagrama de Gantt, e por processo chegada, término, turnaround,
CPU, I/O e espera (direta e cruzada), mais o turnaround médio e o tempo
médio de espera.

## Resultado do exemplo do enunciado (`configs/cenario_c.txt`)

```
t  : 0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19
CPU: P01 P01 P02 P02 P02 P02 P01 P01 --  P02 P02 P01 P02 P02 P02 --  --  --  P02 P02

t  : 20  21  22  23  24  25  26  27  28
CPU: P02 P02 P02 --  --  --  P02 P02 P02
```

| Processo | Término | Turnaround | CPU | I/O | Espera |
|---|---|---|---|---|---|
| P01 | 12 | 12 | 5 | 3 | 4 (ticks 2, 3, 4, 5) |
| P02 | 29 | 28 | 17 | 9 | 2 (ticks 1, 11) |

Turnaround médio 20; espera média 3.

### Divergência com o gabarito do PDF

O simulador segue as regras do enunciado, e não o gabarito do PDF, porque
o gabarito é inconsistente com os próprios programas:

- Usa 4 UTs de CPU para P1 e 8 para P2, mas os programas executam **5** e
  **17** instruções, e cada instrução custa 1 UT.
- Agrupa instruções no mesmo instante (STORE e SYSCALL 1 de P2 em t=4) e
  pula o BRPOS em t=8.
- O log coloca `[P1] Finalizado` depois de `[P2] ... 0`, contradizendo a
  própria linha do tempo.

Os valores impressos (15, 2, 1, 0) coincidem com os do simulador.

## Casos de teste

| Configuração | O que demonstra |
|---|---|
| `cenario_a.txt` | P1 sozinho: rebaixamento e bloqueio por I/O. |
| `cenario_b.txt` | P2 sozinho: laço com três impressões. |
| `cenario_c.txt` | Exemplo do enunciado (P1 + P2). |
| `caso1.txt` | Instruções: `SYSCALL 2`, `BRNEG`, `BRZERO`, `BRANY`, `MULT` e `DIV` com negativos. |
| `caso2.txt` | Escalonamento: desempate FIFO na Fila 1 e duas preempções por retorno de I/O, com volta ao topo do grupo e quantum restante. |

O resultado esperado de `caso1.txt` e `caso2.txt`, calculado à mão, está
nos comentários de cada arquivo.

Em `programas/` também estão `figura1.asm` (exemplo de formato do
enunciado) e `erro1.asm` (erro proposital: salto para um label que não
existe, detectado na linha 6).
