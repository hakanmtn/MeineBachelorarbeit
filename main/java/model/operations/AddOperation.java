package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;

public record AddOperation(Integer element) implements CollectionOperation<Integer, Boolean> {

    @Override
    public Boolean execute(Collection<Integer> collection) {
        return collection.add(element);
    }

    @Override
    public String getContractName() {
        return "add";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {

        Boolean boolResult = (Boolean) result;

        BoolExpr correctSequence = context.mkEq(newContent,
                context.mkConcat(oldContent, context.mkUnit(context.mkInt(element))));

        BoolExpr correctResult = context.mkEq(context.mkBool(boolResult), context.mkTrue());

        return context.mkAnd(correctSequence, correctResult);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {

        Boolean boolResult = (Boolean) result;

        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));

        BoolExpr elementExists = context.mkContains(oldContent, elementSeq);
        BoolExpr noChange = context.mkAnd(
                context.mkNot(context.mkBool(boolResult)),
                context.mkEq(newContent, oldContent)
        );
        BoolExpr caseExists = context.mkImplies(elementExists, noChange);

        BoolExpr elementNew = context.mkNot(elementExists);
        BoolExpr wasAdded = context.mkAnd(
                context.mkBool(boolResult),
                context.mkEq(context.mkLength(newContent),
                        context.mkAdd(context.mkLength(oldContent), context.mkInt(1))),
                context.mkContains(newContent, elementSeq)
        );
        BoolExpr caseNew = context.mkImplies(elementNew, wasAdded);

        return context.mkAnd(caseExists, caseNew);
    }
}