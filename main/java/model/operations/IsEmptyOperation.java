package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;

public record IsEmptyOperation() implements CollectionOperation<Integer, Boolean> {

    @Override
    public Boolean execute(Collection<Integer> collection) {
        return collection.isEmpty();
    }

    @Override
    public String getContractName() {
        return "isEmpty";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {
        Boolean boolResult = (Boolean) result;
        return commonIsEmptyContract(oldContent, newContent, boolResult, context);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        Boolean boolResult = (Boolean) result;  // Cast hinzugefügt
        return commonIsEmptyContract(oldContent, newContent, boolResult, context);
    }

    private BoolExpr commonIsEmptyContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                           Boolean result, Context context) {
        BoolExpr collectionUnchanged = context.mkEq(oldContent, newContent);

        Sort intSort = context.getIntSort();
        Sort seqSort = context.mkSeqSort(intSort);
        SeqExpr emptySeq = context.mkEmptySeq(seqSort);

        BoolExpr correctResult = context.mkEq(
                context.mkBool(result),
                context.mkEq(emptySeq, oldContent)
        );

        return context.mkAnd(collectionUnchanged, correctResult);
    }
}