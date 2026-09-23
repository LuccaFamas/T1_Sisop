import java.util.List;

import assembly.AssemblyParser;
import assembly.Instruction;
import cpu.InstructionExecutor;
import cpu.Memory;
import process.Process;

public class Main {

    public static void main(String[] args) {

        Memory memory = new Memory();

        memory.set("valor", 10);

        Process process = new Process(
                "P1",
                0,
                3);


                AssemblyParser parser = new AssemblyParser();

        List<String> code = List.of(
        "LOAD limite",
        "loop:",
        "SUB #1",
        "STORE temp",
        "BRPOS loop",
        "SYSCALL 0"
        );  

        List<Instruction> instructions =
        parser.parseCode(code);

            System.out.println("Labels:");
        System.out.println(parser.getLabels());

        InstructionExecutor executor =
            new InstructionExecutor(
            memory,
            parser.getLabels()
        );

        for (int i = 0; i < instructions.size(); i++) {
        System.out.println(
            i + " -> " + instructions.get(i)
        );
    }
    
        Instruction load = new Instruction("LOAD", "valor");

        Instruction add = new Instruction("ADD", "#5");

        Instruction store = new Instruction("STORE", "valor");

        executor.execute(load, process);

        System.out.println("Depois do LOAD:");
        System.out.println("ACC = " + process.getAcc());
        System.out.println("PC = " + process.getPc());

        executor.execute(add, process);

        System.out.println("\nDepois do ADD:");
        System.out.println("ACC = " + process.getAcc());
        System.out.println("PC = " + process.getPc());

        executor.execute(store, process);

        System.out.println("\nDepois do STORE:");
        System.out.println("ACC = " + process.getAcc());
        System.out.println("PC = " + process.getPc());
        System.out.println("valor = " + memory.get("valor"));

    }
}