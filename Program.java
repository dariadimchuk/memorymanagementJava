import data.AlgorithmType;
import data.Node;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

public class Program {

    public static boolean debug;

    public static void main(String[] args){
        var program = new Program();

        try{
            program.readFiles();
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    public void readFiles() throws Exception {
        System.out.println("Turn on debug? \nDebug ON: Prints on every command & uses test.txt." +
                "\nDebug OFF: Prints only on P command, and uses specified file. \nDebug: (Y/N) ");
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        debug = input.compareToIgnoreCase("Y") == 0 ? true : false;

        var filename = "";
        if(!debug){
            System.out.println("Please enter path for file of text instructions: ");
            filename = in.nextLine();
        } else{
            filename = ".\\..\\test.txt";
        }


        long startTime = System.nanoTime();


        var lines = Files.readAllLines(Paths.get(filename));

        AlgorithmType algorithm = setAlgorithm(Integer.parseInt(lines.get(0)));
        int totalSize = Integer.parseInt(lines.get(1));

        var mm = new MemoryManager(algorithm, totalSize);

        System.out.println(algorithm.toString());
        System.out.println("Memory Size: " + totalSize);
        System.out.println();


        if(debug){
            printDetails(mm.allNodes);
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


    public AlgorithmType setAlgorithm(int i){
        switch (i){
            case 1: return AlgorithmType.First;
            case 2: return AlgorithmType.Best;
            case 3: return AlgorithmType.Worst;
            default: return AlgorithmType.First; //default to first
        }
    }

}


