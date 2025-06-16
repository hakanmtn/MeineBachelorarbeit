package model;

import com.microsoft.z3.*;
import java.util.List;


public interface ListOperation<T, R> {

    R execute(List<T> list);

    String getContractName();

    BoolExpr contract(SeqExpr<IntSort> oldContent, SeqExpr<IntSort> newContent,
                      R result, Context context);
}
