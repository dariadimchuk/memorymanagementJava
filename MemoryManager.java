import data.AlgorithmType;
import data.Node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MemoryManager {

    private AlgorithmType algorithm;
    private int sizeKB;
    private int filledSize;

    public LinkedList<Node> memory;

    public MemoryManager(AlgorithmType type, int memorySize){
        algorithm = type;
        sizeKB = memorySize;

        memory = new LinkedList<>();
        memory.addFirst(new Node(0, sizeKB)); //add an empty node
    }


    public void beginMemoryManagement(List<String> lines){
        for(int i = 2; i < lines.size(); i++){
            //do everything else
            var line = lines.get(i);
            determineFunction(line);

            if(Program.debug){
                Program.printDetails(memory);
            }
        }
    }



    public void determineFunction(String line){
        var instructions = line.split(" ");
        var function = instructions[0];

        //option 3 - print out current state of memory (allocations and free spaces)
        if (function.compareToIgnoreCase("P") == 0) {
            if(Program.debug){
                System.out.println("\nPrinting command");
            } else Program.printDetails(memory);
        } else {
            int processId = Integer.parseInt(instructions[1]);

            //option 2 - deallocate memory for process with id PID
            if (function.compareToIgnoreCase("D") == 0) {
                if(Program.debug){ System.out.println("\nDeallocating process " + processId); }

                deallocate(processId);
            } else if (function.compareToIgnoreCase("A") == 0) { //option 1 - allocate a memory with id PID, that's size MEMORY_SIZE. Unit would be in KBs.
                if(Program.debug){ System.out.println("\nAllocating for process " + processId); }

                int size = Integer.parseInt(instructions[2]);
                genericAllocation(processId, size);
            }
        }

    }

    public void genericAllocation(int id, int size){
        if (algorithm == AlgorithmType.First) {
            if(!firstAllocate(id, size)){
                compaction();
                if(!firstAllocate(id, size)){
                    System.out.println("Fatal error: ran out of space for allocation. " + filledSize + " / " + sizeKB);
                }
            }
        }
        else {
            if(!bestWorstAllocate(id, size)){
                compaction();
                if(!bestWorstAllocate(id, size)){
                    System.out.println("Fatal error: ran out of space for allocation. " + filledSize + " / " + sizeKB);
                }
            }
        }
    }


    public boolean firstAllocate(int id, int size){
        filledSize += size;
        var unknownIndex = -1; //temporarily give unknown index
        var nodeToMake = new Node(id, unknownIndex, size);


        if(memory.size() == 1) { //first insert
            nodeToMake.setStartIndex(0);
            addNode(0, nodeToMake, memory.getFirst());

            return true;
        } else{
            for(int i  = 0; i < memory.size(); i++){
                var curr = memory.get(i);

                nodeToMake.setStartIndex(curr.getStartIndex());

                //if empty and fits our size
                if (curr.willFit(size)) {
                    addNode(i, nodeToMake, curr);
                    return true;
                }
            }
        }

        return false;
    }


    public boolean bestWorstAllocate(int id, int size){
        filledSize += size;
        var unknownIndex = -1; //temporarily give unknown index

        var nodeToMake = new Node(id, unknownIndex, size);

        Node bestNode = null;
        int bestNodeIndex = -1;
        int lastSize = -1;

        //completely empty linkedlist
        if(memory.size() == 1) {
            nodeToMake.setStartIndex(0);
            addNode(0, nodeToMake, memory.getFirst());
            return true;

        } else{
            for(int i  = 0; i < memory.size(); i++){
                var curr = memory.get(i);

                //if empty and fits our size
                if (curr.willFit(size))
                {
                    if(algorithm == AlgorithmType.Best)
                    {
                        //if lastSize wasn't initialized by first linkedlist item
                        if (lastSize == -1 || lastSize >= curr.getSize())
                        {
                            nodeToMake.setStartIndex(curr.getStartIndex());
                            bestNode = curr;
                            bestNodeIndex = i;
                            lastSize = curr.getSize();
                        }
                    } else if(algorithm == AlgorithmType.Worst)
                    {
                        //if lastSize wasn't initialized by first linkedlist item
                        if (lastSize == -1 || lastSize <= curr.getSize())
                        {
                            nodeToMake.setStartIndex(curr.getStartIndex());
                            bestNode = curr;
                            bestNodeIndex = i;
                            lastSize = curr.getSize();
                        }
                    }
                }
            }
        }


        if (bestNodeIndex != -1 && bestNode != null){
            addNode(bestNodeIndex, nodeToMake, bestNode);
            return true;
        }

        return false;
    }


    public void addNode(int indexToAddTo, Node nodeToMake, Node nodeAfter){
        memory.add(indexToAddTo, nodeToMake);

        nodeAfter.setStartIndex(nodeToMake.getStartIndex() + nodeToMake.getSize() + 1);
        nodeAfter.setSize(nodeAfter.getSize() - nodeToMake.getSize() - 1);
    }



    public void compaction(){
        var newList = new LinkedList<Node>();

        var startIndex = 0;
        for(int i = 0; i < memory.size(); i++){
            var node = memory.get(i);
            if(node.isFull()){
                node.setStartIndex(startIndex);
                newList.add(node);
            }

            startIndex += node.getSize() + 1;
        }

        //new compacted node at the end
        int compactedSize = sizeKB - filledSize;
        var compactedNode = new Node(startIndex, compactedSize);

        newList.add(compactedNode);
    }


    public void deallocate(int id){

        for(int index = 0; index < memory.size(); index++){
            var node = memory.get(index);

            if(node.getProcessId() == id){ //found!
                node.releaseNode();

                filledSize -= node.getSize();

                var prev = index > 0 ? memory.get(index - 1) : null;
                var next = memory.get(index + 1);

                var leftMergeNeeded = prev != null && !prev.isFull();
                var rightMergeNeeded = next != null && !next.isFull();
                var bothSidesMergeNeeded = leftMergeNeeded && rightMergeNeeded;

                var anyMergeNeeded = leftMergeNeeded | rightMergeNeeded;

                if (anyMergeNeeded)
                {
                    //in case we need to merge some empty blocks together
                    var start = 0;
                    var size = 0;

                    var indexToAddBeforeTo = -1;

                    var nodesToRemove = new ArrayList<Node>();

                    //if both neighbours on each side is empty, remove and merge sides
                    if (bothSidesMergeNeeded)
                    {
                        start = prev.getStartIndex();
                        size = prev.getSize() + node.getSize() + next.getSize() + 2; //adding 1 counts the zero spot (for both sides)

                        indexToAddBeforeTo = index - 1;

                        nodesToRemove.add(prev);
                        nodesToRemove.add(next);
                        nodesToRemove.add(node);
                    }
                    //if just prev neighbour is empty
                    else if (leftMergeNeeded)
                    {
                        start = prev.getStartIndex();
                        size = prev.getSize() + node.getSize() + 1; //adding 1 counts the zero spot

                        indexToAddBeforeTo = index - 1;

                        nodesToRemove.add(prev);
                        nodesToRemove.add(node);
                    }
                    //if just next neighbour is empty
                    else if (rightMergeNeeded)
                    {
                        start = node.getStartIndex();
                        size = next.getSize() + node.getSize() + 1; //adding 1 counts the zero spot

                        indexToAddBeforeTo = index;

                        nodesToRemove.add(next);
                        nodesToRemove.add(node);
                    }


                    memory.add(indexToAddBeforeTo, new Node(start, size));

                    for (var rm: nodesToRemove) {
                        memory.remove(rm);
                    }

                }//merging area done

                break;
            }//found if statement
        }//end loop
    }


}
