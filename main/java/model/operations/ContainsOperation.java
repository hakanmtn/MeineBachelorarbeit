package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;
import java.util.List;


public record ContainsOperation(Integer element) implements ListOperation<Integer, Boolean> {

    @Override
    public Boolean execute(List<Integer> list) {
        return list.contains(element);
    }

    @Override
    public String getContractName() {
        return "contains";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                             Boolean result, Context context) {
        // Liste bleibt unverändert
        BoolExpr listUnchanged = context.mkEq(newContent, oldContent);

        // Ergebnis entspricht dem Vorhandensein des Elements
        BoolExpr correctResult = context.mkEq(
                context.mkBool(result),
                context.mkContains(oldContent, context.mkUnit(context.mkInt(element)))
        );

        return context.mkAnd(listUnchanged, correctResult);
    }
}
