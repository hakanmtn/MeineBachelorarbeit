package model.operations;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.SeqExpr;
import model.ListOperation;

import java.util.List;

public record RemoveOperation(Integer element) implements ListOperation<Integer,Boolean>{

    @Override
    public Boolean execute(List<Integer> list) {
        return list.remove(element); //erstes Vorkommen von element
    }

    @Override
    public String getContractName() {
        return "remove";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent, Boolean result, Context context) {

        //es gibt 2 Möglichkeiten: element enthält oder nicht

        BoolExpr elementExists = context.mkContains(oldContent, context.mkUnit(context.mkInt(element)));

        //falls enthält      result = true
        BoolExpr wasRemoved = context.mkAnd(elementExists,context.mkBool(result),
                context.mkEq(context.mkLength(newContent), context.mkSub(context.mkLength(oldContent) , context.mkInt(1))));

        //falls nicht enthält   result = false

        BoolExpr wasNotRemoved = context.mkAnd(context.mkNot(elementExists),
                context.mkNot(context.mkBool(result)),
                context.mkEq(newContent,oldContent));


        return context.mkOr(wasRemoved,wasNotRemoved);
    }
}