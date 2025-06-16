package model;

import java.util.ArrayList;
import java.util.List;

//Führt Operationen auf einer Liste aus und zeichnet den Verlauf auf
public class OperationExecutor<T> {
    // Die Liste, auf der Operationen ausgeführt werden
    private final List<T> list;

    // Aufzeichnung der Zustände und Ergebnisse
    private final List<List<T>> states = new ArrayList<>();
    private final List<ListOperation<T, ?>> operations = new ArrayList<>();
    private final List<Object> results = new ArrayList<>();


    public OperationExecutor(List<T> list) {
        this.list = list;
        recordState();
    }

    //Zeichnet den aktuellen Zustand der Liste auf
    private void recordState() {
        // Kopie des aktuellen Zustands speichern
        states.add(new ArrayList<>(list));
    }

    public <R> R executeOperation(ListOperation<T, R> operation) {

        R result = operation.execute(list);

        // Operation und Ergebnis aufzeichnen
        operations.add(operation);
        results.add(result);

        recordState();

        return result;
    }

    public List<List<T>> getStates() {
        return states;
    }

    public List<ListOperation<T, ?>> getOperations() {
        return operations;
    }

    public List<Object> getResults() {
        return results;
    }
}
