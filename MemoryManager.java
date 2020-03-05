import data.AlgorithmType;
import data.Node;

import java.util.*;

public class MemoryManager {

    private AlgorithmType algorithm;
    private int sizeKB;
    private int filledSize;

    public LinkedList<Node> memory;


    public TreeSet<Node> allNodes;
    public TreeSet<Node> emptyNodes;
    public HashMap<Integer, Node> processesMap;

    public MemoryManager(AlgorithmType type, int memorySize){
        algorithm = type;
        sizeKB = memorySize;

        memory = new LinkedList<>();
        var emptyNode = new Node(0, sizeKB);
        memory.addFirst(emptyNode); //add an empty node


        processesMap = new HashMap<>();

        allNodes = new TreeSet<>(getComparatorForAll());
        emptyNodes = new TreeSet<>(getEmptyComparator());

        allNodes.add(emptyNode);
        emptyNodes.add(emptyNode);
    }

    public Comparator<Node> getComparatorForAll(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
            }
        };
    }


    public Comparator<Node> getEmptyComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                switch (algorithm){
                    case First:
                        return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
                    case Best:
                        return Integer.compare(o1.getSize(), o2.getSize());
                    case Worst:
                        return Integer.compare(o1.getSize(), o2.getSize());
                    default:
                        return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
                }
            }
        };
    }




    public void beginMemoryManagement(List<String> lines) throws Exception {
        for(int i = 2; i < lines.size(); i++){
            //do everything else
            var line = lines.get(i);
            determineFunction(line);

            if(Program.debug){
                Program.printDetails(allNodes);
            }
        }
    }



    public void determineFunction(String line) throws Exception {
        var instructions = line.split(" ");
        var function = instructions[0];

        //option 3 - print out current state of memory (allocations and free spaces)
        if (function.compareToIgnoreCase("P") == 0) {
            if(Program.debug){
                System.out.println("\nPrinting command");
            } else Program.printDetails(allNodes);
        } else {
            int processId = Integer.parseInt(instructions[1]);

            //option 2 - deallocate memory for process with id PID
            if (function.compareToIgnoreCase("D") == 0) {
                if(Program.debug){ System.out.println("\nDeallocating process " + processId); }

                deallocate(processId);
            } else if (function.compareToIgnoreCase("A") == 0) { //option 1 - allocate a memory with id PID, that's size MEMORY_SIZE. Unit would be in KBs.
                if(Program.debug){ System.out.println("\nAllocating for process " + processId); }

                int size = Integer.parseInt(instructions[2]);

                if(!allocate(processId, size)){
                    compaction();
                    if(!allocate(processId, size)){
                        throw new Exception("Fatal error: ran out of space for allocation of PID "
                                + processId + ". \" + filledSize + \" / \" + sizeKB");
                    }
                }
            }
        }

    }


    public boolean allocate(int id, int size){
        var nodeToMake = new Node(id, -1, size);

        Node emptyNode = null;

        if(algorithm == AlgorithmType.Best){
            emptyNode = emptyNodes.floor(nodeToMake);
        } else if(algorithm == AlgorithmType.Worst){
            emptyNode = emptyNodes.ceiling(nodeToMake);
        } else {
            emptyNode = emptyNodes.first();
        }


        //found an empty spot that fits
        if(emptyNode != null){
            filledSize += size;

            nodeToMake.setStartIndex(emptyNode.getStartIndex());

            processesMap.put(nodeToMake.getProcessId(), nodeToMake);

            var newStartIndex = nodeToMake.getStartIndex() + nodeToMake.getSize() + 1;
            int leftover = emptyNode.getSize() - nodeToMake.getSize();
            emptyNode.setStartIndex(newStartIndex);
            emptyNode.setSize(leftover);

            if(leftover == 0){
                allNodes.remove(emptyNode);
            }

            allNodes.add(nodeToMake);

            return true;
        } else{
            //no empty spot found
            return false;
        }
    }


    public void compaction(){
        allNodes.removeIf(n -> !n.isFull()); //clear all empties

        var startIndex = allNodes.last().getStartIndex();
        int compactedSize = sizeKB - filledSize;

        //create new merged empty node, with start index at the end, and a combined size
        var mergedEmpty = new Node(startIndex, compactedSize);

        allNodes.add(mergedEmpty);
    }


    public void deallocate(int id) throws Exception {
        if(!processesMap.containsKey(id)){
            throw new Exception("Deallocation: Process " + id + " not found");
        }

        var node = processesMap.get(id);
        filledSize -= node.getSize();

        /*
        * TODO NEED TO CHECK & MERGE NEIGHBOURS
        * */

        node.deallocateNode();

        var prev = allNodes.lower(node);
        var next = allNodes.higher(node);

        var leftMergeNeeded = prev != null && !prev.isFull();
        var rightMergeNeeded = next != null && !next.isFull();
        var bothSidesMergeNeeded = leftMergeNeeded && rightMergeNeeded;

        var anyMergeNeeded = leftMergeNeeded | rightMergeNeeded;


        if (anyMergeNeeded) {
            //in case we need to merge some empty blocks together
            var start = 0;
            var size = 0;

            var nodesToRemove = new ArrayList<Node>();

            //if both neighbours on each side is empty, remove and merge sides
            if (bothSidesMergeNeeded) {
                start = prev.getStartIndex();
                size = prev.getSize() + node.getSize() + next.getSize() + 2; //adding 1 counts the zero spot (for both sides)

                nodesToRemove.add(prev);
                nodesToRemove.add(next);
                nodesToRemove.add(node);
            }
            //if just prev neighbour is empty
            else if (leftMergeNeeded) {
                start = prev.getStartIndex();
                size = prev.getSize() + node.getSize() + 1; //adding 1 counts the zero spot

                nodesToRemove.add(prev);
                nodesToRemove.add(node);
            }
            //if just next neighbour is empty
            else if (rightMergeNeeded) {
                start = node.getStartIndex();
                size = next.getSize() + node.getSize() + 1; //adding 1 counts the zero spot

                nodesToRemove.add(next);
                nodesToRemove.add(node);
            }


            allNodes.removeAll(nodesToRemove);
            emptyNodes.removeAll(nodesToRemove);

            //TODO I dont think node got updated inside allNodes OR emptyNodes :((( Need to search & update in both
            //ANS: we just remove & add it to both!

            node.setStartIndex(start);
            node.setSize(size);
            allNodes.add(node);
            emptyNodes.add(node);
        } else{

            //no merging was done, but we still need to track this guy as an empty node!
            emptyNodes.add(node);
        }

        //TODO before you add this node, we need to remove the ones that require merging....
        //emptyNodes.add(node);
        //allNodes.remove(node); //no need to remove! should be in all, even if empty


    }


}
