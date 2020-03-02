import data.AlgorithmType;
import data.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Program {
    public static final int NO_PROCESS = -1;

    public AlgorithmType type;
    public int sizeKB;
    public int filledSize;

    public LinkedList<Node> memory;

    public static void main(String[] args){
        var program = new Program();
        program.readFiles();

    }


    public void readFiles(){
        System.out.println("Please enter path for file of text instructions: ");

        var filename = "C:\\Users\\Daria\\Documents\\BCIT\\CST\\Term 4\\OS\\lab8-MM-assignment-java\\test.txt";

        try{
            File file = new File(filename);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                int counter = 0;

                while ((line = br.readLine()) != null) {
                    if(counter == 0){
                        setAlgorithm(Integer.parseInt(line));
                    } else if(counter == 1){
                        sizeKB = Integer.parseInt(line);
                        initMemory();
                        System.out.println("Initial look:");
                        printDetails();
                    } else{
                        //do everything else
                        determineFunction(line);
                        printDetails();
                    }

                    counter++;
                }
            }

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void initMemory(){
        //processLocationTable = new HashMap<>();
        memory = new LinkedList<>();

        var first = new Node(NO_PROCESS, 0, sizeKB, false);
        memory.addFirst(first);
    }


    public void determineFunction(String line){
        var instructions = line.split(" ");
        var function = instructions[0];

        //option 3 - print out current state of memory (allocations and free spaces)
        if (function.compareToIgnoreCase("P") == 0) {
            System.out.println("\nPrinting command");
            //printDetails();
        } else {
            int processId = Integer.parseInt(instructions[1]);

            //option 2 - deallocate memory for process with id PID
            if (function.compareToIgnoreCase("D") == 0) {
                System.out.println("\nDeallocating process " + processId);
                deallocate(processId);
            } else if (function.compareToIgnoreCase("A") == 0) { //option 1 - allocate a memory with id PID, that's size MEMORY_SIZE. Unit would be in KBs.

                System.out.println("\nAllocating for process " + processId);
                int size = Integer.parseInt(instructions[2]);
                genericAllocation(processId, size);
            }
        }

    }

    public void genericAllocation(int id, int size){
        if (type == AlgorithmType.First) {
            firstAllocate(id, size);
        }
        else bestWorstAllocate(id, size);
    }


    public void firstAllocate(int id, int size){
        filledSize += size;
        var unknownIndex = -1; //temporarily give unknown index

        var nodeToMake = new Node(id, unknownIndex, size, true);

        var success = false;

        if(memory.size() == 1) { //first insert
            nodeToMake.startIndex = 0;
            memory.add(0, nodeToMake);

            //this node was previously first
            memory.get(1).startIndex = nodeToMake.size + 1;
            memory.get(1).size -= (nodeToMake.size + 1);

            //trackProcessIndex(id, 0);
            success = true;
        } else{
            for(int i  = 0; i < memory.size(); i++){
                var curr = memory.get(i);

                nodeToMake.startIndex = curr.startIndex;

                //if empty and fits our size
                if (!curr.full && curr.size >= size)
                {
                    memory.add(i, nodeToMake);
                    curr.startIndex = nodeToMake.size + 1 + nodeToMake.startIndex;
                    curr.size -= (nodeToMake.size + 1);


                    //trackProcessIndex(id, i);

                    success = true;
                    break;
                }
            }
        }


        if (!success)
        {
            System.out.println("OOps, we ran out of spacE?? " + filledSize + " / " + sizeKB);
            //maybe add call do do compaction???
        }
    }


    public void bestWorstAllocate(int id, int size){
        filledSize += size;
        var unknownIndex = -1; //temporarily give unknown index

        var nodeToMake = new Node(id, unknownIndex, size, true);

        Node bestNode = null;
        int bestNodeIndex = -1;
        int lastSize = -1;

        //completely empty linkedlist
        if(memory.size() == 1) {
            nodeToMake.startIndex = 0;
            memory.add(0, nodeToMake);

            //this node was previously first
            memory.get(1).startIndex = nodeToMake.size + 1;
            memory.get(1).size -= (nodeToMake.size + 1);

            return;

        } else{
            for(int i  = 0; i < memory.size(); i++){
                var curr = memory.get(i);

                nodeToMake.startIndex = curr.startIndex;

                //if empty and fits our size
                if (!curr.full && curr.size >= size)
                {
                    if(type == AlgorithmType.Best)
                    {
                        //if lastSize wasn't initialized by first linkedlist item
                        if (lastSize == -1 || lastSize >= curr.size)
                        {
                            bestNode = curr;
                            bestNodeIndex = i;
                            lastSize = curr.size;
                        }
                    } else if(type == AlgorithmType.Worst)
                    {
                        //if lastSize wasn't initialized by first linkedlist item
                        if (lastSize == -1 || lastSize <= curr.size)
                        {
                            bestNode = curr;
                            bestNodeIndex = i;
                            lastSize = curr.size;
                        }
                    }
                }
            }
        }


        if (bestNodeIndex != -1){
            memory.add(bestNodeIndex, nodeToMake);
            bestNode.startIndex = nodeToMake.size + 1 + nodeToMake.startIndex;
            bestNode.size -= (nodeToMake.size + 1);


            //trackProcessIndex(id, bestNodeIndex);
        } else{
            System.out.println("OOps, we ran out of spacE?? " + filledSize + " / " + sizeKB);
            //maybe add call do do compaction???
        }
    }


    public void deallocate(int id){

        for(int index = 0; index < memory.size(); index++){
            var node = memory.get(index);
            if(node.processId == id){
                node.full = false;
                node.processId = NO_PROCESS;
                filledSize -= node.size;

                var prev = index > 0 ? memory.get(index - 1) : null;
                var next = memory.get(index + 1);

                var leftMergeNeeded = prev != null && !prev.full;
                var rightMergeNeeded = next != null && !next.full;
                var bothSidesMergeNeeded = leftMergeNeeded && rightMergeNeeded;

                var anyMergeNeeded = leftMergeNeeded | rightMergeNeeded;

                if (anyMergeNeeded)
                {
                    //in case we need to merge some empty blocks together
                    var start = 0;
                    var size = 0;

                    var indexToAddBeforeTo = -1;
                    //Node nodeToAddBeforeTo = null;

                    var nodesToRemove = new ArrayList<Node>();

                    //if both neighbours on each side is empty, remove and merge sides
                    if (bothSidesMergeNeeded)
                    {
                        start = prev.startIndex;
                        size = prev.size + node.size + next.size + 1; //adding 1 counts the zero spot

                        indexToAddBeforeTo = index - 1;

                        nodesToRemove.add(prev);
                        nodesToRemove.add(next);
                        nodesToRemove.add(node);
                    }
                    //if just prev neighbour is empty
                    else if (leftMergeNeeded)
                    {
                        start = prev.startIndex;
                        size = prev.size + node.size + 1; //adding 1 counts the zero spot

                        indexToAddBeforeTo = index - 1;

                        nodesToRemove.add(prev);
                        nodesToRemove.add(node);
                    }
                    //if just next neighbour is empty
                    else if (rightMergeNeeded)
                    {
                        start = node.startIndex;
                        size = next.size + node.size + 1; //adding 1 counts the zero spot

                        indexToAddBeforeTo = index;

                        nodesToRemove.add(next);
                        nodesToRemove.add(node);
                    }


                    var newnode = new Node(NO_PROCESS, start, size, false);

                    memory.add(indexToAddBeforeTo, newnode);

                    for (var rm: nodesToRemove) {
                        memory.remove(rm);
                    }

                }//merging area done

                break;
            }//found if statement
        }//end loop
    }


    public void printDetails(){
        for(int i = 0; i < memory.size(); i++){
            var current = memory.get(i);

            var start = current.startIndex;
            var end = current.startIndex + current.size;

            var id = current.processId != NO_PROCESS ? "process " + current.processId : "";
            var state = current.full ? "FULL" : "EMPTY";
            var size = "size " + current.size;


            System.out.println(String.format("[%1$5s - %2$5s] (%3$5s) - %4$10s %5$10s",
                    start, end, state, size, id));
        }
    }


    public void setAlgorithm(int i){
        if(i == 1){
            type = AlgorithmType.First;
            System.out.println("Algorithm is First");
        } else if(i == 2){
            type = AlgorithmType.Best;
            System.out.println("Algorithm is Best");
        } else if(i == 3){
            type = AlgorithmType.Worst;
            System.out.println("Algorithm is Worst");
        } else type = AlgorithmType.First; //default
    }

}


