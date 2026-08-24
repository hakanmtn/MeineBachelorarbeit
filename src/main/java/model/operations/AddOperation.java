package model.operations;

import com.microsoft.z3.*;
import model.CollectionOperation;
import model.Config;
import java.util.Collection;

import static model.Config.Mutant.L1_LIST_ADD_LEN_STAYS;

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

        if (Config.mutant == L1_LIST_ADD_LEN_STAYS) {
            // Falscher Vertrag: new == old, result == true
            BoolExpr wrongSeq = context.mkEq(newContent, oldContent);
            BoolExpr resultTrue = context.mkEq(context.mkBool((Boolean) result), context.mkTrue());
            return context.mkAnd(wrongSeq, resultTrue);
        }

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
        //falls element existiert
        BoolExpr caseExists = context.mkImplies(
                elementExists,
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkFalse()),
                        context.mkEq(newContent, oldContent))
        );

        //falls element neu
        BoolExpr caseNew = context.mkImplies(
                context.mkNot(elementExists),
                context.mkAnd(
                        context.mkEq(context.mkBool(boolResult), context.mkTrue()),
                        context.mkEq(context.mkLength(newContent),
                                context.mkAdd(context.mkLength(oldContent), context.mkInt(1))),
                        context.mkContains(newContent, elementSeq)
                )

        );


        BoolExpr noDuplicates = AddOperation.noDuplicates(newContent, context);

        return context.mkAnd(caseExists, caseNew, noDuplicates);
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
