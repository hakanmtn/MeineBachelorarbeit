package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;

//contains methode für Sets und List gleich
public record ContainsOperation(Integer element) implements CollectionOperation<Integer, Boolean> {

    @Override
    public Boolean execute(Collection<Integer> collection) {
        return collection.contains(element);
    }

    @Override
    public String getContractName() {
        return "contains";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {
        Boolean boolResult = (Boolean) result;
        return commonContainsContract(oldContent, newContent, boolResult, context);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        Boolean boolResult = (Boolean) result;
        return commonContainsContract(oldContent, newContent, boolResult, context);
    }

    private BoolExpr commonContainsContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                            Boolean result, Context context) {
        BoolExpr collectionUnchanged = context.mkEq(oldContent, newContent);

        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));
        BoolExpr correctResult = context.mkEq(
                context.mkBool(result),
                context.mkContains(oldContent, elementSeq)
        );

        return context.mkAnd(collectionUnchanged, correctResult);
    }
}
