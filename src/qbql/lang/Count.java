package qbql.lang;

import qbql.index.NamedTuple;
import qbql.lattice.Relation;
import qbql.lattice.Tuple;

public class Count {

    public static String[] getSymbolicNames() {
    	return new String[] {
        		"Sigma _summands 1 = result",
        		"Σ 1 = result",
        		"result += 1", // need matching attribute
        		"result += 1(summands)",
    	};
    }
	
	
    public static NamedTuple summands_result( 
            Relation summands 
    ) {
        int ret = 0;
        for( Tuple t: summands.getContent() ) {
            ret += 1;
        }
        String[] columns = new String[]{"result"};
        Object[] data = new Object[]{ret};
        return new NamedTuple(columns,data);
    }
    public static NamedTuple summands_i_result( 
            Relation summands 
    ) {
        int ret = 0;
        for( Tuple t: summands.getContent() ) {
            ret += 1;
        }
        String[] columns = new String[]{"result"};
        Object[] data = new Object[]{ret};
        return new NamedTuple(columns,data);
    }
}
