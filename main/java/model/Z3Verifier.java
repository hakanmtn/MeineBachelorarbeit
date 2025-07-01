package model;


import com.microsoft.z3.*;

import java.util.HashMap;
import java.util.List;

public class Z3Verifier {

    public static <T> SeqExpr<IntSort> [] createStateVariablesConstraints(OperationExecutor<T> executor, Solver solver,Context ctx) {

        List<List<T>> states = executor.getStates(); //z.b [[],[5],[5,10],[5,10]] (leer -> add(5) -> add(10) -> contains(5))
        SeqExpr<IntSort> [] contentVars = new SeqExpr[states.size()];

        //variablen erstellen
        for (int i = 0; i < states.size(); i++) {
            contentVars[i] = (SeqExpr<IntSort>) ctx.mkConst("content" + i ,ctx.mkSeqSort(ctx.getIntSort()));
        }
        //zustandsdefinitionen als Constraints hinzufügen
        for (int i = 0; i < states.size(); i++) {

            List<Integer> state = (List<Integer>) states.get(i);

            SeqExpr<IntSort> stateValue = listToZ3Seq(state,ctx);
            //z.b: stateValue = (seq.++(seq.++(as seq empty (Seq Int)) (seq.unit 5)) (seq.unit 10))

            BoolExpr stateConstraint = ctx.mkEq(contentVars[i], stateValue); //content2 = [5,10]
            solver.add(stateConstraint);

            System.out.println("Z3 Java-API: content" + i + " = " + stateValue);

        }
        return contentVars;

    }

    private static SeqExpr<IntSort> listToZ3Seq(List<Integer> list, Context ctx) {

        Sort intSort = ctx.getIntSort();
        Sort seqSort = ctx.mkSeqSort(intSort);

        SeqExpr result = ctx.mkEmptySeq(seqSort);

        for(Integer i : list) {
            IntExpr intValue = ctx.mkInt(i);
            SeqExpr unit = ctx.mkUnit(intValue);


            SeqExpr[] concatArgs = new SeqExpr[]{result, unit};
            result = ctx.mkConcat(concatArgs);

        }
        System.out.println("Result: " + result);
        return result;
    }



    public static <T> boolean verifyOperationHistory(OperationExecutor<T> executor) {
        util.Z3Utils.loadZ3Libraries();

        try(Context ctx = new Context(new HashMap<>())) {
            Solver solver = ctx.mkSolver();

            //Zustandsvariablen erstellen und Historie constraints hinzufügen
            SeqExpr<IntSort> [] contentVars = createStateVariablesConstraints(executor,solver,ctx);

            //Verträge als Z3-Constraints hinzufügen
            List<ListOperation<T,?>> operations = executor.getOperations();
            List<Object> results = executor.getResults();

            for(int i  = 0; i < operations.size(); i++) {
                ListOperation<T,?> operation = operations.get(i);
                Object result = results.get(i);

                BoolExpr contractConstraint;

                if(operation instanceof model.operations.AddOperation){
                    contractConstraint = ((model.operations.AddOperation) operation).contract(contentVars[i],
                            contentVars[i+1], (Boolean) result,ctx);
                }else if (operation instanceof model.operations.RemoveOperation) {
                    contractConstraint = ((model.operations.RemoveOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.ContainsOperation) {
                    contractConstraint = ((model.operations.ContainsOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.GetOperation) {
                    contractConstraint = ((model.operations.GetOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Integer) result, ctx);
                } else if (operation instanceof model.operations.ClearOperation){
                    contractConstraint = operation.contract(contentVars[i], contentVars[i+1], null, ctx );
                } else if (operation instanceof model.operations.IsEmptyOperation) {
                    contractConstraint = ((model.operations.IsEmptyOperation) operation)
                            .contract(contentVars[i], contentVars[i + 1], (Boolean) result, ctx);
                } else if (operation instanceof model.operations.AddIndexOperation) {
                    contractConstraint =  operation
                            .contract(contentVars[i], contentVars[i + 1], null, ctx);
                } else {
                    throw new UnsupportedOperationException("Operation nicht unterstützt: " + operation.getClass());
                }

                solver.add(contractConstraint);
                System.out.println("Contract constraint added for: " + operation.getContractName());
            }

            //Satisfiability prüfen
            Status status = solver.check();
            System.out.println("Z3 Java-API: status: " + status);
            return status == Status.SATISFIABLE;

        }catch (Exception e ){
            System.err.println("Z3 Java-API: error: " + e);
            e.printStackTrace();
            return false;
        }
    }
}





