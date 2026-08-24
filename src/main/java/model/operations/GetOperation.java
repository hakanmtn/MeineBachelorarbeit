package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;
import java.util.List;

public record GetOperation(int index) implements CollectionOperation<Integer, Integer> {

    @Override
    public Integer execute(Collection<Integer> collection) {
        if (!(collection instanceof List)) {
            throw new UnsupportedOperationException("get() ist nur für Listen verfügbar");
        }

        List<Integer> list = (List<Integer>) collection;
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
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {
        Integer intResult = (Integer) result;

        BoolExpr listUnchanged = context.mkEq(newContent, oldContent);

        BoolExpr validIndex = context.mkAnd(
                context.mkGe(context.mkInt(index), context.mkInt(0)),
                context.mkLt(context.mkInt(index), context.mkLength(oldContent))
        );

        BoolExpr correctResult = context.mkEq(
                context.mkInt(intResult),
                context.mkNth(oldContent, context.mkInt(index))
        );

        return context.mkAnd(listUnchanged, validIndex, correctResult);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        throw new UnsupportedOperationException("get() ist nicht für Sets verfügbar");
    }
}