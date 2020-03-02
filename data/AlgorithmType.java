package data;

public enum AlgorithmType {
    First,
    Best,
    Worst;


    @Override
    public String toString() {
        return "\nAlgorithm Type: " + this.name();
    }
}
