package qbql.lang;

import qbql.index.NamedTuple;
import qbql.lattice.Relation;
import qbql.lattice.Tuple;
import qbql.util.Util;

public class Sum {
    public static String[] getSymbolicNames() {
    	return new String[] {
        		"Sigma summands = result",
        		"Σ summands = result",
        		//"result = result + summands",  // conflicts with plus!
        		"result += summands",
        		"result = result + summands[i]",
        		"result += summands[i]",
    	};
    }
	
	
    public static NamedTuple summands_result( 
            Relation summands 
    ) {
        Number ret = 0;
        for( Tuple t: summands.getContent() ) {
            ret = Util.plus(ret, (Number)t.data[0]);
        }
        String[] columns = new String[]{"result"};
        Object[] data = new Object[]{ret};
        return new NamedTuple(columns,data);
    }
    public static NamedTuple summands_i_result( 
            Relation summands 
    ) {
    	Number ret = 0;
        for( Tuple t: summands.getContent() ) {
            ret = Util.plus(ret, (Number)t.data[0]);
        }
        String[] columns = new String[]{"result"};
        Object[] data = new Object[]{ret};
        return new NamedTuple(columns,data);
    }
}
