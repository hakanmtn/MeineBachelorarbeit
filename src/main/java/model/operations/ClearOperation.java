package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;

public record ClearOperation() implements CollectionOperation<Integer, Void> {

    @Override
    public Void execute(Collection<Integer> collection) {
        collection.clear();
        return null;
    }

    @Override
    public String getContractName() {
        return "clear";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {

        return commonClearContract( newContent, context);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {

        return commonClearContract(newContent, context);
    }

    private BoolExpr commonClearContract( SeqExpr<IntSort> newContent,
                                         Context context) {
        IntSort intSort = context.getIntSort();
        Sort seqSort = context.mkSeqSort(intSort);
        SeqExpr emptySeq = context.mkEmptySeq(seqSort);

        return context.mkEq(newContent, emptySeq);
    }
}
