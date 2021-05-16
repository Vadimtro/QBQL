package qbql.lang;

import java.util.HashMap;
import java.util.Map;

import qbql.lattice.Relation;

public class IntegerInterval {

    public static String[] getSymbolicNames() {
    	return new String[] {
        		"for(int i = from; i<to; i++)",
        		"from <= int i < to",
        		"i in [from,...,to)",
        		//"i in {from,...,to-1}", rather cumbersome notation e.g. all squares from 1 to 8: "i in {1,...,9-1}" ^ "i * i = i2";
    	};
    }
    public static Relation from_to_i( int from, int to ) {
        Relation ret = new Relation(
            new String[]{"from","to","i"}
        );
        for( int j = from; j < to; j++ ) {
            Map<String,Object> content = new HashMap<String,Object>();
            content.put("from", from);
            content.put("to", to);
            content.put("i", j);
            ret.addTuple(content);
        }
        return ret;
    }  
}
