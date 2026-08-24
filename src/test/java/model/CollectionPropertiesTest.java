package model;

import model.operations.*;
import net.jqwik.api.*;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static model.Config.Mutant.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CollectionPropertiesTest {

    @Property(tries = 5)
    void arrayListMixedOperationsConformToContracts(
            @ForAll("listMixedOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new ArrayList<>(),
                CollectionOperationExecutor.CollectionType.ARRAY_LIST,
                operations, "ArrayList Mixed Test");
    }

    @Property(tries = 5)
    void arrayListWithIndexOperationsConformToContracts(
            @ForAll("listWithIndexOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new ArrayList<>(),
                CollectionOperationExecutor.CollectionType.ARRAY_LIST,
                operations, "ArrayList Index Test");
    }

    @Property(tries = 10)
    void treeSetOperationsConformToContracts(
            @ForAll("setOperations") List<CollectionOperation<Integer, ?>> operations) {

        testCollection(new TreeSet<>(),
                CollectionOperationExecutor.CollectionType.TREE_SET,
                operations, "TreeSet Test");
    }

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

        for (CollectionOperation<Integer, ?> operation : operations) {
            try {
                executor.executeOperation(operation);
                Statistics.collect(operation.getContractName());
            } catch (Exception exception) {
                Statistics.collect("Exception: " + operation.getContractName());
                Assertions.fail("Unexpected exception while executing " + operation, exception);
            }
        }

        boolean isConform = CollectionZ3Verifier.verifyOperationHistory(executor);

        Assertions.assertTrue(isConform,
                "The " + type + " execution history does not conform to its contracts in " + testName);
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
                    List<CollectionOperation<Integer, ?>> operations = new ArrayList<>();
                    for (Integer element : initialElements) {
                        operations.add(new AddOperation(element));
                    }

                    return Arbitraries.integers().between(0, 10)
                            .list().ofMinSize(1).ofMaxSize(10)
                            .map(newElements -> {
                                int currentSize = initialElements.size();

                                for (Integer element : newElements) {
                                    int validIndex = Math.abs(element % (currentSize + 1));
                                    operations.add(new AddIndexOperation(validIndex, element));
                                    currentSize++;
                                }

                                int getIndex = Math.abs(newElements.get(0) % currentSize);
                                operations.add(new GetOperation(getIndex));
                                return operations;
                            });
                });
    }

    @Provide
    Arbitrary<List<CollectionOperation<Integer, ?>>> setOperations() {
        Arbitrary<Integer> values = Arbitraries.integers().between(1, 5);

        Arbitrary<CollectionOperation<Integer, ?>> addOp = values.map(AddOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> containsOp = values.map(ContainsOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> removeOp = values.map(RemoveOperation::new);
        Arbitrary<CollectionOperation<Integer, ?>> clearOp = Arbitraries.just(new ClearOperation());
        Arbitrary<CollectionOperation<Integer, ?>> isEmptyOp = Arbitraries.just(new IsEmptyOperation());

        Arbitrary<CollectionOperation<Integer, ?>> anySetOp = Arbitraries.oneOf(
                addOp, containsOp, removeOp, clearOp, isEmptyOp);

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

    @Test
    void unsatWhenListAddContractClaimsLengthStays() {
        Config.mutant = L1_LIST_ADD_LEN_STAYS;
        try {
            CollectionOperationExecutor<Integer> executor =
                    new CollectionOperationExecutor<>(new ArrayList<>(),
                            CollectionOperationExecutor.CollectionType.ARRAY_LIST);

            executor.executeOperation(new AddOperation(1));

            boolean isConform = CollectionZ3Verifier.verifyOperationHistory(executor);
            assertFalse(isConform, "The mutated List.add contract must be UNSAT");
        } finally {
            Config.mutant = NONE;
        }
    }

    @Test
    void unsatWhenEncoderDropsLastElement() {
        Config.mutant = E1_ENCODER_DROP_LAST_ELEMENT;
        try {
            CollectionOperationExecutor<Integer> executor =
                    new CollectionOperationExecutor<>(new ArrayList<>(),
                            CollectionOperationExecutor.CollectionType.ARRAY_LIST);

            executor.executeOperation(new AddOperation(2));

            boolean isConform = CollectionZ3Verifier.verifyOperationHistory(executor);
            assertFalse(isConform, "The mutated encoder must be UNSAT");
        } finally {
            Config.mutant = NONE;
        }
    }

    @Test
    void unsatWhenHistoryFlipsContainsResult() {
        Config.mutant = H1_HISTORY_FLIP_CONTAINS_RESULT;
        try {
            CollectionOperationExecutor<Integer> executor =
                    new CollectionOperationExecutor<>(new ArrayList<>(),
                            CollectionOperationExecutor.CollectionType.ARRAY_LIST);

            executor.executeOperation(new AddOperation(5));
            executor.executeOperation(new ContainsOperation(5));

            boolean isConform = CollectionZ3Verifier.verifyOperationHistory(executor);
            assertFalse(isConform, "The manipulated contains result must be UNSAT");
        } finally {
            Config.mutant = NONE;
        }
    }
}
