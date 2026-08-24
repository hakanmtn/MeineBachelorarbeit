package model;

import com.microsoft.z3.*;
import java.util.*;

import static model.Config.Mutant.E1_ENCODER_DROP_LAST_ELEMENT;

public class CollectionZ3Verifier {

    public static <T> boolean verifyOperationHistory(CollectionOperationExecutor<T> executor) {
        util.Z3Utils.loadZ3Libraries();
        System.out.println("Config: mutant=" + Config.mutant + ", type=" + executor.getType());

        try (Context ctx = new Context(new HashMap<>())) {
            Solver solver = ctx.mkSolver();

            // Zustandsvariablen erstellen
            SeqExpr<IntSort>[] contentVars = createStateVariablesConstraints(executor, solver, ctx);


            List<CollectionOperation<T,?>> operations = executor.getOperations();
            List<Object> results = executor.getResults();
            List<Collection<T>> states = executor.getStates();


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

                if (Config.VERBOSE) {
                    System.out.printf("op[%d]: %s, result=%s%n", i + 1, String.valueOf(operation), String.valueOf(result));
                    System.out.printf("JAVA σ%d -> σ%d : %s -> %s%n", i, i + 1, states.get(i), states.get(i + 1));
                    System.out.printf("SMT  contract[%d] = %s%n",
                            i, contractConstraint.simplify().toString());
                }

                // Mit Label tracken, damit Unsat-Core sinnvoll ist
                BoolExpr label = ctx.mkBoolConst("op_" + i + "_" + operation.getContractName());
                solver.assertAndTrack(contractConstraint, label);


                if (!Config.VERBOSE) {
                    System.out.println("Contract constraint added for: " + operation.getContractName()
                            + " (" + executor.getType() + ")");
                }
            }

            long t0 = System.nanoTime();
            Status status = solver.check();
            long ms = (System.nanoTime() - t0) / 1_000_000L; // Millisekunden
            System.out.println("Z3 Verification (" + executor.getType() + "): " + status + " in " + ms + " ms");
            if (status == Status.UNSATISFIABLE) {
                try {
                    BoolExpr[] core = solver.getUnsatCore();
                    System.out.print("UNSAT core labels: ");
                    System.out.println(Arrays.stream(core).map(Expr::toString).toList());
                } catch (Exception ignore) {}
            }
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

            if (Config.VERBOSE) {
                System.out.printf("JAVA σ%d = %s%n", i, state);
                System.out.printf("SMT  content%d := %s%n", i, stateValue.toString());
            } else {
                System.out.println("Z3: content" + i + " = " + stateValue);
            }


            BoolExpr stateConstraint = ctx.mkEq(contentVars[i], stateValue);
            BoolExpr label = ctx.mkBoolConst("state_" + i);
            solver.assertAndTrack(stateConstraint, label);

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


        // Mutierter Übersetzer: letztes Element „verlieren“
        if (Config.mutant == E1_ENCODER_DROP_LAST_ELEMENT && !collection.isEmpty()) {

            // len(result) - 1
            ArithExpr lenMinusOne = ctx.mkSub(ctx.mkLength(result), ctx.mkInt(1));
            @SuppressWarnings("unchecked")
            SeqExpr<IntSort> dropped =
                    (SeqExpr<IntSort>) ctx.mkExtract(result, ctx.mkInt(0), lenMinusOne);

            result = dropped;
        }
        return result;
    }
}