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
    @Property(tries = 10)
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
        return Arbitraries.create(() -> {
            Random random = new Random();
            List<CollectionOperation<Integer, ?>> operations = new ArrayList<>();

            // Erste Operation: immer add
            operations.add(new AddOperation(random.nextInt(10) + 1));

            // 1-9 weitere Operationen
            int numOps = random.nextInt(9) + 1;
            for (int i = 0; i < numOps; i++) {
                int value = random.nextInt(10) + 1;
                switch (random.nextInt(5)) {
                    case 0 -> operations.add(new AddOperation(value));
                    case 1 -> operations.add(new ContainsOperation(value));
                    case 2 -> operations.add(new RemoveOperation(value));
                    case 3 -> operations.add(new ClearOperation());
                    case 4 -> operations.add(new IsEmptyOperation());
                }
            }
            return operations;
        });
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> listWithIndexOperations() {
        return Arbitraries.create(() -> {
            Random random = new Random();
            List<CollectionOperation<Integer, ?>> operations = new ArrayList<>();

            List<Integer> simulatedList = new ArrayList<>();

            // Erstelle Basis-Liste mit 1-5 Elementen
            int baseSize = random.nextInt(5) + 1;
            for (int i = 0; i < baseSize; i++) {
                int value = random.nextInt(20) + 1;
                operations.add(new AddOperation(value));
                simulatedList.add(value);
            }

            // Füge 1-5 Index-Operationen hinzu
            int numIndexOps = random.nextInt(5) + 1;
            for (int i = 0; i < numIndexOps; i++) {
                int value = random.nextInt(20) + 1;

                int validIndex = random.nextInt(simulatedList.size() + 1);

                operations.add(new AddIndexOperation(validIndex, value));

                simulatedList.add(validIndex, value);
            }

            if (!simulatedList.isEmpty()) {
                int getIndex = random.nextInt(simulatedList.size());
                operations.add(new GetOperation(getIndex));
            }

            return operations;
        });
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> setOperations() {
        Arbitrary<Integer> values = Arbitraries.integers().between(1,5);

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