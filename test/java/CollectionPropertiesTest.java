import model.*;
import model.operations.*;
import net.jqwik.api.*;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.Assertions;
import java.util.*;

class CollectionPropertiesTest {


    // ArrayList Tests
    @Property(tries = 5)
    void arrayListMixedOperationsConformToContracts(
            @ForAll("listMixedOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new ArrayList<>(),
                CollectionOperationExecutor.CollectionType.ARRAY_LIST, //LINKED_LIST
                operations, "ArrayList Mixed Test");
    }

    @Property(tries = 5)
    void arrayListWithIndexOperationsConformToContracts(
            @ForAll("listWithIndexOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new ArrayList<>(),
                CollectionOperationExecutor.CollectionType.ARRAY_LIST, //LINKED_LIST
                operations, "ArrayList Index Test");
    }

    // TreeSet Tests
    @Property(tries = 5)
    void treeSetOperationsConformToContracts(
            @ForAll("setOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new TreeSet<>(),
                CollectionOperationExecutor.CollectionType.TREE_SET,
                operations, "TreeSet Test");
    }

    // HashSet tests
    @Property(tries = 5)
    void hashSetOperationsConformToContracts(
            @ForAll("setOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new HashSet<>(),
                CollectionOperationExecutor.CollectionType.HASH_SET,
                operations, "HashSet Test");
    }

    private void testCollection(Collection<Integer> collection,
                                CollectionOperationExecutor.CollectionType type,
                                List<CollectionOperation<Integer, ?>> operations,
                                String testName) {

        CollectionOperationExecutor<Integer> executor =
                new CollectionOperationExecutor<>(collection, type);


        for (CollectionOperation<Integer, ?> op : operations) {
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
        System.out.println("Endzustand: " + collection);
        System.out.println("==============================");

        // Die Historie mit Z3 verifizieren
        boolean isConform = CollectionZ3Verifier.verifyOperationHistory(executor);

        if (!isConform) {
            System.err.println("Konformitätsprüfung fehlgeschlagen für: " + testName);
            System.err.println("Operationen: " + operations);
            System.err.println("Endzustand: " + collection);
            System.err.println("Zustände: " + executor.getStates());
            System.err.println("Ergebnisse: " + executor.getResults());
        }

        Assertions.assertTrue(isConform,
                "Die " + type + "-Implementierung entspricht nicht den formalen Verträgen bei: " + testName);
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> listMixedOperations() {
        Arbitrary<Integer> values = Arbitraries.integers().between(1, 10);

        Arbitrary<CollectionOperation<Integer, ?>> addOp = values.map(AddOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> containsOp = values.map(ContainsOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> removeOp = values.map(RemoveOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> clearOp = Arbitraries.just(new ClearOperation());
        Arbitrary<CollectionOperation<Integer, ?>> isEmptyOp = Arbitraries.just(new IsEmptyOperation());

        Arbitrary<CollectionOperation<Integer, ?>> anyOp = Arbitraries.oneOf(
                addOp, containsOp, removeOp, clearOp, isEmptyOp);

        // Sequenz: Erste Operation ist Add, dann 1-4 beliebige Operationen
        return addOp.flatMap(firstAdd ->
                anyOp.list().ofMinSize(1).ofMaxSize(10)
                        .map(restOps -> {
                            List<CollectionOperation<Integer, ?>> result = new ArrayList<>();
                            result.add(firstAdd);
                            result.addAll(restOps);
                            return result;
                        })
        );
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> listWithIndexOperations() {
        return Arbitraries.integers().between(1, 20)
                .list().ofMinSize(1).ofMaxSize(5)
                .flatMap(initialElements -> {
                    // Erst normale Add-Operationen für Basis-Liste -- ansonsten Exception !!
                    List<CollectionOperation<Integer, ?>> operations = new ArrayList<>();
                    for (Integer elem : initialElements) {
                        operations.add(new AddOperation(elem));
                    }

                    // Dann AddIndex-Operationen mit gültigen Indizes
                    return Arbitraries.integers().between(0, 10)
                            .list().ofMinSize(1).ofMaxSize(10)
                            .map(newElements -> {
                                int currentSize = initialElements.size();

                                for (Integer elem : newElements) {
                                    // Gültiger Index: 0 <= index <= currentSize
                                    int validIndex = Math.abs(elem % (currentSize + 1));
                                    operations.add(new AddIndexOperation(validIndex, elem));
                                    currentSize++;
                                }

                                //Ein paar Get-Operationen hinzufügen
                                if (currentSize > 0) {
                                    int getIndex = Math.abs(newElements.get(0) % currentSize);
                                    operations.add(new GetOperation(getIndex));
                                }

                                return operations;
                            });
                });
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> setOperations() {
        Arbitrary<Integer> values = Arbitraries.integers().between(1, 10);

        Arbitrary<CollectionOperation<Integer, ?>> addOp = values.map(AddOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> containsOp = values.map(ContainsOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> removeOp = values.map(RemoveOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> clearOp = Arbitraries.just(new ClearOperation());
        Arbitrary<CollectionOperation<Integer, ?>> isEmptyOp = Arbitraries.just(new IsEmptyOperation());

        // Nur Set-Operationen (keine GetOperation, AddIndexOperation)
        Arbitrary<CollectionOperation<Integer, ?>> anySetOp = Arbitraries.oneOf(
                addOp, containsOp, removeOp, clearOp, isEmptyOp);

        // Sequenz: Erste Operation ist Add, dann 1-4 beliebige Set-Operationen
        return addOp.flatMap(firstAdd ->
                anySetOp.list().ofMinSize(1).ofMaxSize(10)
                        .map(restOps -> {
                            List<CollectionOperation<Integer, ?>> result = new ArrayList<>();
                            result.add(firstAdd);
                            result.addAll(restOps);
                            return result;
                        })
        );
    }
}