package data;

public class Node {
    public int processId;
    public int startIndex;
    public int size;
    public boolean full;

    public Node(int id, int index, int size, boolean full){
        processId = id;
        startIndex = index;
        this.size = size;
        this.full = full;
    }
}
