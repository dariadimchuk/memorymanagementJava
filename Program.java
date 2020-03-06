import data.AlgorithmType;
import data.Node;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeSet;

public class Program {

    public static boolean debug;

    public static void main(String[] args){
        var program = new Program();

        try{
            program.readFiles(args[0]);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    public void readFiles(String filename) throws Exception {

        debug = false; //if true, prints results at EVERY line

        long startTime = System.nanoTime();


        var lines = Files.readAllLines(Paths.get(filename));

        AlgorithmType algorithm = setAlgorithm(Integer.parseInt(lines.get(0)));
        int totalSize = Integer.parseInt(lines.get(1));

        var mm = new MemoryManager(algorithm, totalSize);

        System.out.println(algorithm.toString());
        System.out.println("Memory Size: " + totalSize);
        System.out.println();


        if(debug){
            printDetails(mm.getMemory());
        }

        //start management

        mm.beginMemoryManagement(lines);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        System.out.println("\n\nEnd of program. Time elapsed: " + durationMs + " ms");
    }


    public static void printDetails(TreeSet<Node> nodes){

        for (Node n : nodes) {
            System.out.println(n);
        }

        System.out.println();
    }


    private AlgorithmType setAlgorithm(int i){
        switch (i){
            case 1: return AlgorithmType.First;
            case 2: return AlgorithmType.Best;
            case 3: return AlgorithmType.Worst;
            default: return AlgorithmType.First; //default to first
        }
    }

}


