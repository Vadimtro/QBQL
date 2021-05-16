package qbql.symbolic;

import java.util.List;
import java.util.Set;

public interface Expr {
    Expr left();
    Expr right();
    String operation();
    Expr substitute( String x, Expr treeNode );
    List<Expr> substitute( List<Expr> src, boolean grow );
    Set<String> variables();
    int complexity();
}
