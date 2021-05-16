package qbql.lang;

import qbql.lattice.Database;
import qbql.lattice.Relation;

public class Integers {

    public static String[] getSymbolicNames() {
    	return new String[] {
        		//"for(int i = -infinity;; i++)",
        		"int i",
        		"i in {...,-1,0,1,...}",
    	};
    }
    public static Relation i( Object i ) {
        if( i instanceof Integer || i instanceof Long ||
            i instanceof Double && Math.abs((Double)i-Math.floor((Double)i)) < Double.MIN_VALUE
        )
        	return Database.R01;
        else
        	return Database.R00;
    }  
}
