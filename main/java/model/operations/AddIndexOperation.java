package model.operations;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.SeqExpr;
import model.ListOperation;

import java.util.List;

public record AddIndexOperation(int index, Integer element) implements ListOperation<Integer, Void>{


    @Override
    public Void execute(List<Integer> list) {
        list.add(index,element);  // nicht boolean, sondern gibt void zurück.
        return null;
    }

    @Override
    public String getContractName() {
        return "add_index";
    }

    @Override
    public BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent, Void result, Context context) {

        //vorbedingung
        BoolExpr precondition = context.mkAnd(context.mkGe(context.mkInt(index), context.mkInt(0)),
                context.mkLe(context.mkInt(index), context.mkLength(oldContent)));

        //nachbedingung prefix, element, suffix
        SeqExpr<IntSort> prefix = context.mkExtract(oldContent, context.mkInt(0), context.mkInt(index));

        //element als einzeln Sequenz
        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));

        //suffix
        SeqExpr<IntSort> suffix = context.mkExtract(oldContent, context.mkInt(index), context.mkLength(oldContent));

        // prefix + element + suffix
        SeqExpr<IntSort> expectedNewContent = context.mkConcat(prefix, elementSeq, suffix);

        BoolExpr postcondition = context.mkEq(newContent, expectedNewContent);

        return context.mkImplies(precondition, postcondition);
    }
}