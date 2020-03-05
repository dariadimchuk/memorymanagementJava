import data.AlgorithmType;
import data.Node;

import java.util.*;

public class MemoryManager {

    private AlgorithmType algorithm;
    private int sizeKB;

    public TreeSet<Node> allNodes;
    public TreeSet<Node> emptyNodes;
    public HashMap<Integer, Node> processesMap;

    public MemoryManager(AlgorithmType type, int memorySize){
        algorithm = type;
        sizeKB = memorySize;

        processesMap = new HashMap<>();
        allNodes = new TreeSet<>(getDefaultComparator());

        var bestOrWorstAlg = algorithm == AlgorithmType.Best || algorithm == AlgorithmType.Worst;
        var comparator = bestOrWorstAlg ? getSizeComparator() : getDefaultComparator();
        emptyNodes = new TreeSet<>(comparator);

        var emptyNode = new Node(0, sizeKB);

        allNodes.add(emptyNode);
        emptyNodes.add(emptyNode);
    }



    public Comparator<Node> getDefaultComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
            }
        };
    }


    public Comparator<Node> getSizeComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(o1.getSize(), o2.getSize());
            }
        };
    }




    public void beginMemoryManagement(List<String> lines) throws Exception {
        for(int i = 2; i < lines.size(); i++){
            //do everything else
            var line = lines.get(i);
            determineFunction(line);
            //debugEmpties();

            if(Program.debug){
                Program.printDetails(allNodes);
            }
        }
    }



    public void determineFunction(String line) {
        var instructions = line.split(" ");
        var function = instructions[0];


        if (function.compareToIgnoreCase("P") == 0) {
            if(Program.debug){
                System.out.println("\nPrinting command");
            } else Program.printDetails(allNodes);
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

    }


    public boolean allocate(int id, int size){
        var nodeToMake = new Node(id, -1, size);

        Node foundSpace = null;

        if(algorithm == AlgorithmType.Best){
            foundSpace = emptyNodes.ceiling(nodeToMake);
        } else if(algorithm == AlgorithmType.Worst){
            var reverse = emptyNodes.descendingSet();
            foundSpace = reverse.first();

            if(foundSpace.getSize() < size){
                foundSpace = null;
            }
        } else {
            foundSpace = emptyNodes.first(); //start at beginning

            //search sequentially for one that fits
            boolean noFit = foundSpace != null ? foundSpace.getSize() < size : true;
            int count = 0;
            while(count < emptyNodes.size() && noFit){
                foundSpace = foundSpace == null ? null : emptyNodes.higher(foundSpace); //hop up
                noFit = foundSpace != null ? foundSpace.getSize() < size : true;
                count++;
            }
        }


        //found an empty spot that fits
        if(foundSpace != null){
            nodeToMake.setStartIndex(foundSpace.getStartIndex());

            processesMap.put(nodeToMake.getProcessId(), nodeToMake);

            //must be removed and added again so it is sorted properly after its size is changed (leftover)
            emptyNodes.remove(foundSpace);


            var newStartIndex = nodeToMake.getStartIndex() + nodeToMake.getSize();
            int leftover = foundSpace.getSize() - nodeToMake.getSize();
            foundSpace.setStartIndex(newStartIndex);
            foundSpace.setSize(leftover);

            if(leftover == 0){
                allNodes.remove(foundSpace);
            } else emptyNodes.add(foundSpace);

            allNodes.add(nodeToMake);

            return true;
        } else return false; //no empty spot found
    }


    public void compaction(){
        allNodes.removeIf(n -> !n.isFull()); //clear all empties

        var node = allNodes.first();
        int i = 0, nextIndex = 0, totalSize = 0;

        while(i < allNodes.size()){
            if(node != null){
                node.setStartIndex(nextIndex);
                nextIndex = node.getStartIndex() + node.getSize();

                totalSize += node.getSize();

                node = allNodes.higher(node);
                i++;
            } else break;
        }


        var lastNode = allNodes.last();
        var startIndex = lastNode.getStartIndex() + lastNode.getSize();
        int compactedSize = sizeKB - totalSize;

        //TODO ideally remove all +1 indices. Its fucked. might be caused by deallocate() subtracting incorrectly ???

        //create new merged empty node, with start index at the end, and a combined size
        var mergedEmpty = new Node(startIndex, compactedSize);

        allNodes.add(mergedEmpty);
        emptyNodes.clear();
        emptyNodes.add(mergedEmpty);
    }


    public void deallocate(int id)  {
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
                //in case we need to merge some empty blocks together
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

                //TODO there is a size different - empty nodes sometimes has more, when allnodes has only 1 empty :(((((
                allNodes.removeAll(nodesToRemove);
                emptyNodes.removeAll(nodesToRemove);

                //TODO I dont think node got updated inside allNodes OR emptyNodes :((( Need to search & update in both
                //ANS: we just remove & add it to both!

                node.setStartIndex(start);
                node.setSize(size);
                allNodes.add(node);
            }

            emptyNodes.add(node); //add to empty nodes regardless whether we merged or not
            processesMap.remove(id);
        } //else System.out.print(" - Process " + id + " not found");
    }


}
