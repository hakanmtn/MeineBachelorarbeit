package model;

import com.microsoft.z3.*;
import java.util.Collection;

public interface CollectionOperation<T, R> {

    R execute(Collection<T> collection);
    String getContractName();

    BoolExpr listContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                          Object result, Context context);

    BoolExpr setContract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                         Object result, Context context);


}