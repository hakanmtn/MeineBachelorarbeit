package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;
import java.util.List;

public record AddIndexOperation(int index, Integer element) implements CollectionOperation<Integer, Void> {

    @Override
    public Void execute(Collection<Integer> collection) {
        if (!(collection instanceof List)) {
            throw new UnsupportedOperationException("add(index, element) ist nur für Listen verfügbar");
        }

        List<Integer> list = (List<Integer>) collection;
        list.add(index, element);
        return null;
    }

    @Override
    public String getContractName() {
        return "add_index";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {

        BoolExpr precondition = context.mkAnd(
                context.mkGe(context.mkInt(index), context.mkInt(0)),
                context.mkLe(context.mkInt(index), context.mkLength(oldContent))
        );

        SeqExpr<IntSort> prefix = context.mkExtract(oldContent, context.mkInt(0), context.mkInt(index));
        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));
        SeqExpr<IntSort> suffix = context.mkExtract(oldContent, context.mkInt(index), context.mkLength(oldContent));

        SeqExpr<IntSort> expectedNewContent = context.mkConcat(prefix, elementSeq, suffix);
        BoolExpr postcondition = context.mkEq(newContent, expectedNewContent);

        return context.mkImplies(precondition, postcondition);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        throw new UnsupportedOperationException("add(index, element) ist nicht für Sets verfügbar");
    }
}