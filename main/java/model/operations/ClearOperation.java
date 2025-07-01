package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;

import java.util.List;

public record ClearOperation() implements ListOperation<Integer,Void> {

    @Override
    public Void execute(List<Integer> list) {
        list.clear();
        return null;
    }

    @Override
    public String getContractName() {
        return "clear";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent, Void result, Context context) {

        IntSort intSort = context.getIntSort();
        Sort seqSort = context.mkSeqSort(intSort);

        SeqExpr emptySeq = context.mkEmptySeq(seqSort);

        return context.mkEq(newContent,emptySeq);
    }
}