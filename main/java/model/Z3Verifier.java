package model;

import com.microsoft.z3.*;
import java.util.HashMap;
import java.util.List;

//Verifiziert Operationshistorien mit Z3 und Java-Verträgen
public class Z3Verifier {

    private static <T> String listToSMTSeqSimple(List<T> list) {
        if (list.isEmpty()) {
            return "(as seq.empty (Seq Int))";
        }

        String result = "(as seq.empty (Seq Int))";
        for (T item : list) {
            result = "(seq.++ " + result + " (seq.unit " + item + "))";
        }

        return result;
    }

    public static <T> String generateSMTLibStates(OperationExecutor<T> executor) {
        StringBuilder smt = new StringBuilder();

        // Header
        smt.append("(set-logic ALL)\n\n");

        // Zustandsdeklarationen
        List<List<T>> states = executor.getStates();
        for (int i = 0; i < states.size(); i++) {
            smt.append(String.format("(declare-const content%d (Seq Int))\n", i));
        }
        smt.append("\n");

        // Zustandsdefinitionen
        for (int i = 0; i < states.size(); i++) {
            smt.append(String.format("(assert (= content%d %s))\n",
                    i, listToSMTSeqSimple(states.get(i))));
        }
        smt.append("\n");
        System.out.println(smt);
        return smt.toString();
    }

    //Verifiziert eine Operationshistorie mit Z3 unter Verwendung von Java-Verträgen
    public static <T> boolean verifyOperationHistory(OperationExecutor<T> executor) {
        util.Z3Utils.loadZ3Libraries();

        try (Context ctx = new Context(new HashMap<>())) {
            Solver solver = ctx.mkSolver();

            // SMT-Lib Zustände parsen
            String smtStates = generateSMTLibStates(executor);
            BoolExpr[] stateAssertions = ctx.parseSMTLIB2String(smtStates, null, null, null, null);
            for (BoolExpr assertion : stateAssertions) {
                System.out.println("Z3 parsed: " + assertion);
                solver.add(assertion);
            }

            // Zustandsvariablen aus dem Context holen
            List<List<T>> states = executor.getStates();
            SeqExpr<IntSort>[] contentVars = new SeqExpr[states.size()];
            for (int i = 0; i < states.size(); i++) {
                contentVars[i] = (SeqExpr<IntSort>) ctx.mkConst("content" + i, ctx.mkSeqSort(ctx.getIntSort()));
            }

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

            }
            System.out.println(solver);

            // Satisfiability prüfen
            Status status = solver.check();
            return status == Status.SATISFIABLE;

        } catch (Exception e) {
            System.err.println("Z3-Fehler: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
