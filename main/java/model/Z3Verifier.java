package model;

import com.microsoft.z3.*;
import java.util.HashMap;
import java.util.List;


public class Z3Verifier {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SeqExpr<IntSort> listToZ3Seq(List<Integer> list, Context ctx) {

        Sort intSort = ctx.getIntSort();
        Sort seqSort = ctx.mkSeqSort(intSort);

        SeqExpr result = ctx.mkEmptySeq(seqSort);

        for (Integer item : list) {

            IntExpr intValue = ctx.mkInt(item);

            SeqExpr unit = ctx.mkUnit(intValue);


            SeqExpr[] concatArgs = new SeqExpr[]{result, unit};
            result = ctx.mkConcat(concatArgs);
        }

        return result;
    }

    public static <T> SeqExpr<IntSort>[] createStateVariablesAndConstraints(
            OperationExecutor<T> executor, Solver solver, Context ctx) {

        List<List<T>> states = executor.getStates();
        SeqExpr<IntSort>[] contentVars = new SeqExpr[states.size()];

        // Variablen erstellen
        for (int i = 0; i < states.size(); i++) {
            contentVars[i] = (SeqExpr<IntSort>) ctx.mkConst("content" + i, ctx.mkSeqSort(ctx.getIntSort()));
        }

        // Zustandsdefinitionen als Constraints hinzufügen
        for (int i = 0; i < states.size(); i++) {
            @SuppressWarnings("unchecked")
            List<Integer> state = (List<Integer>) states.get(i);

            SeqExpr<IntSort> stateValue = listToZ3Seq(state, ctx);
            BoolExpr stateConstraint = ctx.mkEq(contentVars[i], stateValue);
            solver.add(stateConstraint);

            System.out.println("Z3 Java-API: content" + i + " = " + stateValue);
        }

        return contentVars;
    }

    public static <T> boolean verifyOperationHistory(OperationExecutor<T> executor) {
        util.Z3Utils.loadZ3Libraries();

        try (Context ctx = new Context(new HashMap<>())) {
            Solver solver = ctx.mkSolver();

            // Zustandsvariablen erstellen und Historie-Constraints hinzufügen
            SeqExpr<IntSort>[] contentVars = createStateVariablesAndConstraints(executor, solver, ctx);

            // Java-Verträge als Z3-Constraints hinzufügen
            List<ListOperation<T, ?>> operations = executor.getOperations();
            List<Object> results = executor.getResults();

            for (int i = 0; i < operations.size(); i++) {
                ListOperation<T, ?> operation = operations.get(i);
                Object result = results.get(i);

                // Contract-Methode aufrufen (Cast nötig wegen Generics)
                BoolExpr contractConstraint;
                if (operation instanceof model.operations.AddOperation) {
                    contractConstraint = ((model.operations.AddOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.RemoveOperation) {
                    contractConstraint = ((model.operations.RemoveOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.ContainsOperation) {
                    contractConstraint = ((model.operations.ContainsOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.GetOperation) {
                    contractConstraint = ((model.operations.GetOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Integer) result, ctx);
                } else {
                    throw new UnsupportedOperationException("Operation nicht unterstützt: " + operation.getClass());
                }

                solver.add(contractConstraint);
                System.out.println("Contract constraint added for: " + operation.getContractName());
            }

            // Satisfiability prüfen
            Status status = solver.check();
            System.out.println("Z3 Status: " + status);
            return status == Status.SATISFIABLE;

        } catch (Exception e) {
            System.err.println("Z3-Fehler: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}