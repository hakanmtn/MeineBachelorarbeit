package model;

import java.util.*;

public class CollectionOperationExecutor<T> {

    public enum CollectionType {
        ARRAY_LIST,
        LINKED_LIST,
        TREE_SET,
        HASH_SET,
    }

    private final Collection<T> collection;
    private final CollectionType type;
    private final List<Collection<T>> states = new ArrayList<>();
    private final List<CollectionOperation<T,?>> operations = new ArrayList<>();
    private final List<Object> results = new ArrayList<>();

    public CollectionOperationExecutor(Collection<T> collection, CollectionType type) {
        this.collection = collection;
        this.type = type;
        recordState();
    }

    private void recordState() {
        // Je nach Typ die richtige Kopie erstellen
        switch(type) {
            case ARRAY_LIST, LINKED_LIST -> {
                // Listen: Reihenfolge beibehalten
                states.add(new ArrayList<>(collection));
            }
            case TREE_SET -> {
                // TreeSet: ist bereits sortiert
                states.add(new TreeSet<>(collection));
            }
            case HASH_SET -> {
                states.add(new HashSet<>(collection));
            }
        }
    }

    public <R> R executeOperation(CollectionOperation<T,R> operation) {
        R result = operation.execute(collection);
        operations.add(operation);
        results.add(result);
        recordState();
        return result;
    }

    public CollectionType getType() { return type; }
    public List<Collection<T>> getStates() { return states; }
    public List<CollectionOperation<T,?>> getOperations() { return operations; }
    public List<Object> getResults() { return results; }
}