package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import java.util.Collection;

public record RemoveOperation(Integer element) implements CollectionOperation<Integer, Boolean> {

    @Override
    public Boolean execute(Collection<Integer> collection) {
        return collection.remove(element);
    }

    @Override
    public String getContractName() {
        return "remove";
    }

    @Override
    public BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                 Object result, Context context) {
        Boolean boolResult = (Boolean) result;

        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));

        BoolExpr elementExists = context.mkContains(oldContent, elementSeq);
        BoolExpr wasRemoved = context.mkAnd(
                context.mkBool(boolResult),
                context.mkEq(context.mkLength(newContent),
                        context.mkSub(context.mkLength(oldContent), context.mkInt(1)))
        );
        BoolExpr caseExists = context.mkImplies(elementExists, wasRemoved);

        BoolExpr elementDoesNotExist = context.mkNot(elementExists);
        BoolExpr noChange = context.mkAnd(
                context.mkNot(context.mkBool(boolResult)),
                context.mkEq(newContent, oldContent)
        );
        BoolExpr caseDoesNotExist = context.mkImplies(elementDoesNotExist, noChange);

        return context.mkAnd(caseExists, caseDoesNotExist);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        Boolean boolResult = (Boolean) result;

        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));

        BoolExpr elementExists = context.mkContains(oldContent, elementSeq);
        BoolExpr wasRemoved = context.mkAnd(
                context.mkBool(boolResult),
                context.mkEq(context.mkLength(newContent),
                        context.mkSub(context.mkLength(oldContent), context.mkInt(1))),
                context.mkNot(context.mkContains(newContent, elementSeq))
        );
        BoolExpr caseExists = context.mkImplies(elementExists, wasRemoved);

        BoolExpr elementDoesNotExist = context.mkNot(elementExists);
        BoolExpr noChange = context.mkAnd(
                context.mkNot(context.mkBool(boolResult)),
                context.mkEq(newContent, oldContent)
        );
        BoolExpr caseDoesNotExist = context.mkImplies(elementDoesNotExist, noChange);

        return context.mkAnd(caseExists, caseDoesNotExist);
    }
}