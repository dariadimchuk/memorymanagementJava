package data;

public class Node implements Comparable<Node> {
    public static final int NO_PROCESS = -1;

    private int processId;
    private int startIndex;
    private int size;
    private boolean full;


    /**
     * Creates a Node that is FULL, and represents an allocated process.
     * @param id - process Id
     * @param index - starting index in linked list
     * @param size - starting index in linked list
     */
    public Node(int id, int index, int size){
        processId = id;
        startIndex = index;
        this.size = size;
        this.full = true;
    }


    /**
     * Creates a Node that is EMPTY, and has no process Id.
     * @param index - starting index in linked list
     * @param size - starting index in linked list
     */
    public Node(int index, int size){
        processId = NO_PROCESS;
        startIndex = index;
        this.size = size;
        this.full = false;
    }


    public int getProcessId(){
        return this.processId;
    }


    public int getSize(){
        return this.size;
    }


    public void setSize(int size){
        this.size = size;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int index){
        this.startIndex = index;
    }

    public boolean isFull() {
        return full;
    }

    /**
     * Adds a process to this node and sets it as Full
     * @param processId
     */
    public void createProcessForNode(int processId){
        this.processId = processId;
        this.full = true;
    }

    /**
     * Removes a process from this node and sets it as Empty.
     */
    public void releaseNode(){
        this.processId = NO_PROCESS;
        this.full = false;
    }


    /**
     * If this node is not already full, and its size will fit the passed-in size, it will fit.
     * @param size
     * @return T or F for whether it will fit
     */
    public boolean willFit(int size){
        if(!this.full && this.size >= size){
            return true;
        } else return false;
    }


    @Override
    public int compareTo(Node o) {
        return Integer.compare(this.size, o.size);
    }


    @Override
    public String toString(){
        var end = startIndex + size;
        var id = processId != NO_PROCESS ? "process " + processId : "";
        var state = full ? "FULL" : "EMPTY";
        var size = "size " + this.size;


        return String.format("[%1$5s - %2$5s] (%3$5s) - %4$10s %5$10s",
                startIndex, end, state, size, id);
    }

}
