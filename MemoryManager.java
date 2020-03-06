import data.AlgorithmType;
import data.Node;
import java.util.*;

public class MemoryManager {

    private AlgorithmType algorithm;
    private int sizeKB;

    private TreeSet<Node> allNodes;
    private TreeSet<Node> emptyNodes;
    private HashMap<Integer, Node> processesMap;

    /**
     * Creates a MemoryManager which tracks a mock memory and assigns processes to certain areas of the
     * memory, depending on the chosen algorithm.
     * @param type - algorithm type. First, Best or Worst
     * @param memorySize - total size of memory
     */
    public MemoryManager(AlgorithmType type, int memorySize){
        algorithm = type;
        sizeKB = memorySize;

        processesMap = new HashMap<>();
        allNodes = new TreeSet<>(getDefaultComparator()); //track all nodes in the order they are

        var bestOrWorstAlg = algorithm == AlgorithmType.Best || algorithm == AlgorithmType.Worst;
        var comparator = bestOrWorstAlg ? getSizeComparator() : getDefaultComparator();
        emptyNodes = new TreeSet<>(comparator);

        var emptyNode = new Node(0, sizeKB);

        allNodes.add(emptyNode);
        emptyNodes.add(emptyNode);
    }



    private Comparator<Node> getDefaultComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
            }
        };
    }


    private Comparator<Node> getSizeComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(o1.getSize(), o2.getSize());
            }
        };
    }


    /**
     * Reads the passed lines, parses into instructions to allocate/deallocate processes,
     * and runs memory management for these processes.
     * @param lines - lines from file
     */
    public void beginMemoryManagement(List<String> lines) {
        for(int i = 2; i < lines.size(); i++){

            var line = lines.get(i);
            determineFunction(line);

            if(Program.debug){
                Program.printDetails(allNodes);
            }
        }
    }


    /**
     * Gets the current memory, with the full or empty processes in it.
     * @return tree set of processes/empty spaces in the memory
     */
    public TreeSet<Node> getMemory(){
        return allNodes;
    }


    /**
     * Determines what function should get called on each instruction.
     * @param line - a single line of instructions (allote/deallocate process or print)
     */
    private void determineFunction(String line) {
        var instructions = line.split(" ");
        var function = instructions[0];


        if (function.compareToIgnoreCase("P") == 0) {
            if(Program.debug){
                System.out.println("\nPrinting command");
            }

            Program.printDetails(allNodes);
        } else {
            int processId = Integer.parseInt(instructions[1]);

            if (function.compareToIgnoreCase("D") == 0) {
                if(Program.debug){ System.out.println("\nDeallocating process " + processId); }

                deallocate(processId);
            } else if (function.compareToIgnoreCase("A") == 0) {
                if(Program.debug){ System.out.println("\nAllocating for process " + processId); }

                int size = Integer.parseInt(instructions[2]);

                if(!allocate(processId, size)){
                    compaction();
                    if(!allocate(processId, size)){
                        System.out.println("Cannot allocate process " + processId);
                    }
                }
            }
        }

    }//end method



    /**
     * Allocate a process to memory.
     * @param id - id of process
     * @param size - size of process
     * @return - boolean on whether allocation succeeded
     */
    private boolean allocate(int id, int size){
        var nodeToMake = new Node(id, -1, size);

        Node foundSpace = findEmptySpace(nodeToMake);

        if(foundSpace != null){
            insertNode(nodeToMake, foundSpace);
            return true;
        } else return false; //no empty spot found

    }//end method



    /**
     * Finds the empty space for the node we're trying to add, based on the program's selected Algorithm type.
     *
     * First - first node that fits.
     * Best - smallest node that fits.
     * Worst - largest node that fits.
     * @param nodeToMake - the node we're trying to add
     * @return - node we're
     */
    private Node findEmptySpace(Node nodeToMake){
        if(algorithm == AlgorithmType.Best){
            return emptyNodes.ceiling(nodeToMake);
        }


        if(algorithm == AlgorithmType.Worst){
            var foundSpace = emptyNodes.last();

            //check if the biggest node can fit the node we're trying to add
            if(foundSpace.getSize() < nodeToMake.getSize()){
                foundSpace = null;
            }

            return foundSpace;
        }


        if(algorithm == AlgorithmType.First){
            var foundSpace = emptyNodes.first(); //start at beginning

            //search sequentially for one that fits
            boolean noFit = foundSpace != null ? foundSpace.getSize() < nodeToMake.getSize() : true;
            int count = 0;

            //loop until you find a node that fits the one we're trying to add (or till we reach the end)
            while(count < emptyNodes.size() && noFit){
                foundSpace = foundSpace == null ? null : emptyNodes.higher(foundSpace); //hop up
                noFit = foundSpace != null ? foundSpace.getSize() < nodeToMake.getSize() : true;
                count++;
            }

            return foundSpace;
        }

        return null;
    }



    /**
     * Adds the process to the list of all nodes.
     * @param nodeToMake - process we're adding
     * @param foundSpace - empty space in memory we're trying to add this process to
     */
    private void insertNode(Node nodeToMake, Node foundSpace){
        nodeToMake.setStartIndex(foundSpace.getStartIndex());

        //track newly added process in hashmap for easier deallocation later
        processesMap.put(nodeToMake.getProcessId(), nodeToMake);

        //the empty space MUST be removed and added again so it is sorted properly after its size is changed (leftover)
        emptyNodes.remove(foundSpace);

        var newStartIndex = nodeToMake.getStartIndex() + nodeToMake.getSize();
        int leftover = foundSpace.getSize() - nodeToMake.getSize();
        foundSpace.setStartIndex(newStartIndex);
        foundSpace.setSize(leftover);


        if(leftover == 0){
            //remove from memory if it is completely filled-in (its size is now 0)
            allNodes.remove(foundSpace);
        } else {
            //if there is any space leftover, it should be re-added to emptyNodes & thereby sorted in the accurate spot
            emptyNodes.add(foundSpace);
        }

        allNodes.add(nodeToMake);
    }//end method


    /**
     * Compacts the empty spaces to be just one merged empty space at the end of the memory.
     * This is done if processes can no longer be added, because the memory is very broken up & too small.
     */
    private void compaction(){
        allNodes.removeIf(n -> !n.isFull()); //clear all empties

        var node = allNodes.first();
        int i = 0, nextIndex = 0, totalSize = 0;

        //for each filled-in node, we need to update the starting index now that we removed all the empty nodes
        while(i < allNodes.size()){
            if(node != null){
                node.setStartIndex(nextIndex);
                nextIndex = node.getStartIndex() + node.getSize();

                totalSize += node.getSize(); //track total filled-in size

                node = allNodes.higher(node);
                i++;
            } else break;
        }


        var lastNode = allNodes.last();
        var startIndex = lastNode.getStartIndex() + lastNode.getSize();
        int compactedSize = sizeKB - totalSize;

        //create new merged empty node, with start index at the end, and a combined size
        var mergedEmpty = new Node(startIndex, compactedSize);

        allNodes.add(mergedEmpty);
        emptyNodes.clear(); //remove all empty nodes, and add the merged one
        emptyNodes.add(mergedEmpty);

    }//end method


    /**
     * Deallocate the specified process.
     * Sets it's boolean flag to empty, and merges any of its neighbours, if they are also empty, into 1 empty node.
     * @param id - process id
     */
    private void deallocate(int id)  {
        if(processesMap.containsKey(id)){
            var node = processesMap.get(id);
            node.deallocateNode();

            //check if we need to merge together empty nodes
            var prev = allNodes.lower(node);
            var next = allNodes.higher(node);

            var leftMergeNeeded = prev != null && !prev.isFull();
            var rightMergeNeeded = next != null && !next.isFull();
            var bothSidesMergeNeeded = leftMergeNeeded && rightMergeNeeded;

            var anyMergeNeeded = leftMergeNeeded | rightMergeNeeded;

            if (anyMergeNeeded) {
                var start = 0;
                var size = 0;

                var nodesToRemove = new ArrayList<Node>();

                //if both neighbours on each side is empty, remove and merge sides
                if (bothSidesMergeNeeded) {
                    start = prev.getStartIndex();
                    size = prev.getSize() + node.getSize() + next.getSize(); //adding 1 counts the zero spot (for both sides)

                    nodesToRemove.add(prev);
                    nodesToRemove.add(next);
                    nodesToRemove.add(node);
                }
                //if just prev neighbour is empty
                else if (leftMergeNeeded) {
                    start = prev.getStartIndex();
                    size = prev.getSize() + node.getSize(); //adding 1 counts the zero spot

                    nodesToRemove.add(prev);
                    nodesToRemove.add(node);
                }
                //if just next neighbour is empty
                else if (rightMergeNeeded) {
                    start = node.getStartIndex();
                    size = next.getSize() + node.getSize(); //adding 1 counts the zero spot

                    nodesToRemove.add(next);
                    nodesToRemove.add(node);
                }

                allNodes.removeAll(nodesToRemove);
                emptyNodes.removeAll(nodesToRemove);

                node.setStartIndex(start);
                node.setSize(size);

                allNodes.add(node);
            }

            emptyNodes.add(node); //add to empty nodes regardless whether we merged or not
            processesMap.remove(id); //remove process from hashmap (that's how we can tell whether its in allNodes)
        }
    }//end method


}//end class
