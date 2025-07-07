package model;

import com.microsoft.z3.*;
import java.util.*;

public class CollectionZ3Verifier {

    public static <T> boolean verifyOperationHistory(CollectionOperationExecutor<T> executor) {
        util.Z3Utils.loadZ3Libraries();

        try (Context ctx = new Context(new HashMap<>())) {
            Solver solver = ctx.mkSolver();

            // Zustandsvariablen erstellen
            SeqExpr<IntSort>[] contentVars = createStateVariablesConstraints(executor, solver, ctx);

            // Verträge als Z3-Constraints hinzufügen
            List<CollectionOperation<T,?>> operations = executor.getOperations();
            List<Object> results = executor.getResults();

            for (int i = 0; i < operations.size(); i++) {
                CollectionOperation<T,?> operation = operations.get(i);
                Object result = results.get(i);

                BoolExpr contractConstraint;

                // Je nach Collection-Typ den entsprechenden Vertrag verwenden
                switch (executor.getType()) {
                    case ARRAY_LIST, LINKED_LIST -> {
                        contractConstraint = operation.listContract(
                                contentVars[i], contentVars[i + 1], result, ctx);
                    }
                    case TREE_SET, HASH_SET -> {
                        contractConstraint = operation.setContract(
                                contentVars[i], contentVars[i + 1], result, ctx);
                    }
                    default -> throw new UnsupportedOperationException("Type: " + executor.getType());
                }

                solver.add(contractConstraint);
                System.out.println("Contract constraint added for: " + operation.getContractName()
                        + " (" + executor.getType() + ")");
            }

            Status status = solver.check();
            System.out.println("Z3 Verification (" + executor.getType() + "): " + status);
            return status == Status.SATISFIABLE;

        } catch (Exception e) {
            System.err.println("Z3 Verification error: " + e);
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static <T> SeqExpr<IntSort>[] createStateVariablesConstraints(
            CollectionOperationExecutor<T> executor, Solver solver, Context ctx) {

        List<Collection<T>> states = executor.getStates();
        SeqExpr<IntSort>[] contentVars = new SeqExpr[states.size()];

        // Variablen erstellen
        for (int i = 0; i < states.size(); i++) {
            contentVars[i] = (SeqExpr<IntSort>) ctx.mkConst("content" + i,
                    ctx.mkSeqSort(ctx.getIntSort()));
        }

        // Zustandsdefinitionen als Constraints hinzufügen
        for (int i = 0; i < states.size(); i++) {
            Collection<Integer> state = (Collection<Integer>) states.get(i);
            SeqExpr<IntSort> stateValue = collectionToZ3Seq(state, ctx);

            BoolExpr stateConstraint = ctx.mkEq(contentVars[i], stateValue);
            solver.add(stateConstraint);

            System.out.println("Z3: content" + i + " = " + stateValue);
        }

        return contentVars;
    }

    private static SeqExpr<IntSort> collectionToZ3Seq(Collection<Integer> collection, Context ctx) {
        Sort intSort = ctx.getIntSort();
        Sort seqSort = ctx.mkSeqSort(intSort);

        SeqExpr result = ctx.mkEmptySeq(seqSort);


        for (Integer i : collection) {
            IntExpr intValue = ctx.mkInt(i);
            SeqExpr unit = ctx.mkUnit(intValue);
            SeqExpr[] concatArgs = new SeqExpr[]{result, unit};
            result = ctx.mkConcat(concatArgs);
        }

        //System.out.println("Collection to Seq: " + result);
        return result;
    }
}