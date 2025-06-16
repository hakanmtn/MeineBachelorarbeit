package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;
import java.util.List;


public record GetOperation(int index) implements ListOperation<Integer, Integer> {

    @Override
    public Integer execute(List<Integer> list) {
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
        }
        return list.get(index);
    }

    @Override
    public String getContractName() {
        return "get";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                             Integer result, Context context) {
        // Liste bleibt unverändert
        BoolExpr listUnchanged = context.mkEq(newContent, oldContent);

        // Vorbedingung: Index muss gültig sein
        BoolExpr validIndex = context.mkAnd(
                context.mkGe(context.mkInt(index), context.mkInt(0)),
                context.mkLt(context.mkInt(index), context.mkLength(oldContent))
        );

        // Ergebnis entspricht dem Element an der Position
        BoolExpr correctResult = context.mkEq(
                context.mkInt(result),
                context.mkNth(oldContent, context.mkInt(index))
        );

        return context.mkAnd(listUnchanged, validIndex, correctResult);
    }
}