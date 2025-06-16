import model.*;
import model.operations.*;
import net.jqwik.api.*;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.Assertions;
import java.util.ArrayList;
import java.util.List;


class ArrayListPropertiesTest {

    @Property(tries = 10)
    void arrayListAddOperationsConformToJavaContracts(
            @ForAll("addOnlySequences") List<ListOperation<Integer, ?>> operations) {

        executeAndVerify(operations, "Add-Only Test");
    }

    //Test mit gemischten Operationen (Add, Contains, Remove)
    @Property(tries = 15)
    void arrayListMixedOperationsConformToJavaContracts(
            @ForAll("mixedOperationsSequences") List<ListOperation<Integer, ?>> operations) {

        executeAndVerify(operations, "Mixed Operations Test");
    }

    //Test mit Add + Get Operationen (erfordert nicht-leere Liste)
    @Property(tries = 10)
    void arrayListAddAndGetOperationsConformToJavaContracts(
            @ForAll("addAndGetSequences") List<ListOperation<Integer, ?>> operations) {

        executeAndVerify(operations, "Add + Get Test");
    }

    // Gemeinsame Ausführungs- und Verifikationslogik
    private void executeAndVerify(List<ListOperation<Integer, ?>> operations, String testName) {

        List<Integer> list = new ArrayList<>();
        OperationExecutor<Integer> executor = new OperationExecutor<>(list);

        // Operationen ausführen
        for (ListOperation<Integer, ?> op : operations) {
            try {
                executor.executeOperation(op);
                Statistics.collect(op.getContractName());
            } catch (Exception e) {
                Statistics.collect("Exception: " + op.getContractName());
                System.err.println("Exception bei " + op + ": " + e.getMessage());
                return;
            }
        }


        System.out.println("=== " + testName + " ===");
        System.out.println("Operationen: " + operations);
        System.out.println("Endzustand: " + list);
        System.out.println("==============================");

        // Die Historie mit Z3 und Java-Verträgen verifizieren
        boolean isConform = Z3Verifier.verifyOperationHistory(executor);

        // Bei Fehlern Details ausgeben
        if (!isConform) {
            System.err.println("Konformitätsprüfung fehlgeschlagen für: " + testName);
            System.err.println("Operationen: " + operations);
            System.err.println("Endzustand: " + list);
            System.err.println("Zustände: " + executor.getStates());
            System.err.println("Ergebnisse: " + executor.getResults());
        }

        // Die Property sollte immer erfüllt sein, wenn unsere Verträge korrekt sind
        Assertions.assertTrue(isConform,
                "Die ArrayList-Implementierung entspricht nicht den formalen Java-Verträgen bei: " + testName);
    }

    @Provide
    Arbitrary<List<ListOperation<Integer, ?>>> addOnlySequences() {
        Arbitrary<ListOperation<Integer, ?>> addOp = Arbitraries.integers()
                .between(1, 100)
                .map(AddOperation::new);

        return addOp.list()
                .ofMinSize(1)
                .ofMaxSize(3);
    }

    //Generiert gemischte Operationen: Add, Contains, Remove
    // Strategie: Mindestens ein Add am Anfang, dann gemischt
    @Provide
    Arbitrary<List<ListOperation<Integer, ?>>> mixedOperationsSequences() {
        Arbitrary<Integer> values = Arbitraries.integers().between(1, 100);

        Arbitrary<ListOperation<Integer, ?>> addOp = values.map(AddOperation::new);

        Arbitrary<ListOperation<Integer, ?>> containsOp = values.map(ContainsOperation::new);

        Arbitrary<ListOperation<Integer, ?>> removeOp = values.map(RemoveOperation::new);

        // Alle Operations gemischt
        Arbitrary<ListOperation<Integer, ?>> anyOp = Arbitraries.oneOf(addOp, containsOp, removeOp);

        // Sequenz: Erste Operation ist Add, dann 1-4 beliebige Operationen
        return addOp.flatMap(firstAdd ->
                anyOp.list().ofMinSize(1).ofMaxSize(4)
                        .map(restOps -> {
                            List<ListOperation<Integer, ?>> result = new ArrayList<>();
                            result.add(firstAdd);  // Erste Operation: Add
                            result.addAll(restOps); // Rest: beliebige Operationen
                            return result;
                        })
        );
    }

    @Provide
    Arbitrary<List<ListOperation<Integer, ?>>> addAndGetSequences() {
        return Arbitraries.integers().between(1, 10)
                .list().ofMinSize(1).ofMaxSize(3)
                .flatMap(values -> {
                    // Erst alle Add-Operationen
                    List<ListOperation<Integer, ?>> ops = new ArrayList<>();
                    for (Integer value : values) {
                        ops.add(new AddOperation(value));
                    }

                    // Dann 1-2 Get-Operationen mit gültigen Indizes
                    int listSize = values.size();
                    return Arbitraries.integers().between(0, listSize - 1)
                            .list().ofMinSize(1).ofMaxSize(2)
                            .map(indices -> {
                                List<ListOperation<Integer, ?>> result = new ArrayList<>(ops);
                                for (Integer index : indices) {
                                    result.add(new GetOperation(index));
                                }
                                return result;
                            });
                });
    }
}