package model;


import java.util.ArrayList;
import java.util.List;

//Bounded Trace Checking
public class OperationExecutor<T> {

    private final List<T> list;

    private final List<List<T>> states = new ArrayList<>();
    private final List<ListOperation<T,?>> operations = new ArrayList<>();
    private final List<Object> results = new ArrayList<>();

    public OperationExecutor(List<T> list) {
        this.list = list;
        recordState();
    }

    private void recordState() {
        states.add(new ArrayList<>(list));
    }

    public <R> R executeOperation(ListOperation<T,R> operation) {
        R result = operation.execute(list);

        operations.add(operation);
        results.add(result);

        recordState();
        return result;

    }

    public List<List<T>> getStates() {
        return states;
    }

    public List<ListOperation<T,?>> getOperations() {
        return operations;
    }

    public List<Object> getResults() {
        return results;
    }


}