package model.operations;

import com.microsoft.z3.*;
import model.ListOperation;
import java.util.List;

public record RemoveOperation(Integer element) implements ListOperation<Integer, Boolean> {

    @Override
    public Boolean execute(List<Integer> list) {
        return list.remove(element);
    }

    @Override
    public String getContractName() {
        return "remove";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                             Boolean result, Context context) {
        // Wenn Element enthalten war: new = old ohne erstes Vorkommen von element
        // Wenn Element nicht enthalten war: new = old
        BoolExpr elementExists = context.mkContains(oldContent, context.mkUnit(context.mkInt(element)));

        // Fall 1: Element war vorhanden -> wurde entfernt -> result = true
        BoolExpr wasRemoved = context.mkAnd(
                elementExists,
                context.mkBool(result),
                // Vereinfachte Bedingung: Länge ist um 1 reduziert
                context.mkEq(context.mkLength(newContent),
                        context.mkSub(context.mkLength(oldContent), context.mkInt(1)))
        );

        // Fall 2: Element war nicht vorhanden -> nichts geändert -> result = false
        BoolExpr wasNotRemoved = context.mkAnd(
                context.mkNot(elementExists),
                context.mkNot(context.mkBool(result)),
                context.mkEq(newContent, oldContent)
        );

        return context.mkOr(wasRemoved, wasNotRemoved);
    }
}