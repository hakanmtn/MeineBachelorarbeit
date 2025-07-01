package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;

import java.util.List;

public record IsEmptyOperation() implements ListOperation<Integer,Boolean>{

    @Override
    public Boolean execute(List<Integer> list) {
        return list.isEmpty();
    }

    @Override
    public String getContractName() {
        return "isEmpty";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent, Boolean result, Context context) {

        BoolExpr listUnchanged = context.mkEq(oldContent,newContent);

        Sort intSort = context.getIntSort();
        Sort seqSort = context.mkSeqSort(intSort);

        SeqExpr emptySeq = context.mkEmptySeq(seqSort);

        BoolExpr correctResult = context.mkEq(context.mkBool(result), context.mkEq(emptySeq,oldContent));

        return context.mkAnd(listUnchanged,correctResult);
    }
}