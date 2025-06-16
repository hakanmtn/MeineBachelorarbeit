package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;
import java.util.List;

public record AddOperation(Integer element) implements ListOperation<Integer, Boolean> {

    @Override
    public Boolean execute(List<Integer> list) {
        return list.add(element);
    }

    @Override
    public String getContractName() {
        return "add";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                             Boolean result, Context context) {
        // new = old ++ [element]
        BoolExpr correctSequence = context.mkEq(
                newContent,
                context.mkConcat(oldContent, context.mkUnit(context.mkInt(element)))
        );

        // result = true
        BoolExpr correctResult = context.mkEq(
                context.mkBool(result),
                context.mkTrue()
        );

        return context.mkAnd(correctSequence, correctResult);
    }
}