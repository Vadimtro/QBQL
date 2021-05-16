package qbql.lang;

import qbql.index.NamedTuple;
import qbql.lattice.Relation;
import qbql.lattice.Tuple;
import qbql.util.Util;

public class Max {
    public static String[] getSymbolicNames() {
        return new String[] {
                "\\/ items = result",
                "v items = result",
                "result v= items",
        };
    }
    
    
    public static NamedTuple items_result( 
            Relation items 
    ) {
        Number ret = 0;
        for( Tuple t: items.getContent() ) {
            ret = Util.max(ret, (Number)t.data[0]);
        }
        String[] columns = new String[]{"result"};
        Object[] data = new Object[]{ret};
        return new NamedTuple(columns,data);
    }
 }
