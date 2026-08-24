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

        //element existiert
        BoolExpr caseExist = context.mkImplies(
                elementExists,
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkTrue()),
                        context.mkEq(context.mkLength(newContent),
                                context.mkSub(context.mkLength(oldContent),context.mkInt(1)))
                )

        );


        BoolExpr caseDoesNotExist = context.mkImplies(
                context.mkNot(elementExists),
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkFalse()),
                        context.mkEq(newContent, oldContent)
                )
        );

        return context.mkAnd(caseExist, caseDoesNotExist);
    }

    @Override
    public BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                                Object result, Context context) {
        Boolean boolResult = (Boolean) result;

        SeqExpr<IntSort> elementSeq = context.mkUnit(context.mkInt(element));

        BoolExpr elementExists = context.mkContains(oldContent, elementSeq);

        //elementExistiert
        BoolExpr caseExists = context.mkImplies(
                elementExists,
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkTrue()),
                        context.mkEq(context.mkLength(newContent),
                                context.mkSub(context.mkLength(oldContent),context.mkInt(1))),
                        context.mkNot(context.mkContains(newContent, elementSeq))

                )
        );


       //element nicht existiert
        BoolExpr caseDoesNotExist = context.mkImplies(
                context.mkNot(elementExists),
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkFalse()),
                        context.mkEq(newContent, oldContent)
                )
        );

        BoolExpr noDuplicates = RemoveOperation.noDuplicates(newContent, context);
        return context.mkAnd(caseExists, caseDoesNotExist, noDuplicates);
    }

    private static BoolExpr noDuplicates(SeqExpr<IntSort> s, Context context) {
        // ∀ i, j. (0 ≤ i < j < len(s)) ⇒ (s[i] ≠ s[j])     i und j sind ganzezahlen

        //(forall ((i Int)) (j Int)) ... )
        Sort Z = context.getIntSort(); //Ganze Zahlen, Integer Typ
        Symbol[] names = new Symbol[] {context.mkSymbol("i"), context.mkSymbol("j")};
        Sort[] sorts = new Sort[] {Z, Z}; //names[0] hat  den Typ sorts[0] = Integer

        //references für z3
        ArithExpr i = (ArithExpr) context.mkBound(1,Z);   //Variable i
        ArithExpr j = (ArithExpr) context.mkBound(0,Z);

        BoolExpr constraints1 = context.mkLe(context.mkInt(0), i); // 0 < i
        BoolExpr constraints2 = context.mkLt(i, j);  // i < j
        BoolExpr constraints3 = context.mkLt(j, context.mkLength(s)); //  j < len(s)


        BoolExpr preCondition = context.mkAnd(constraints1,constraints2,constraints3);

        //(s[i] ≠ s[j]) = ¬(s[i] = s[j])
        Expr element_i = context.mkNth(s, i); //wie bei get()
        Expr element_j = context.mkNth(s, j);

        BoolExpr postCondition = context.mkNot(context.mkEq(element_i,element_j));

        // (0 ≤ i < j < len(s)) ⇒ (s[i] ≠ s[j])
        BoolExpr body  = context.mkImplies(preCondition,postCondition);

        return context.mkForall(sorts,names,body,0, null,null,null,null);



    }
}
