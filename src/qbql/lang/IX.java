package qbql.lang;

import qbql.lattice.Database;
import qbql.lattice.Relation;

public class IX {

    public static Relation lft_rgt( Object lft, Object rgt ) {
    	if( !(lft instanceof Relation) || !(rgt instanceof Relation) )
    		throw new AssertionError("LE !lft/rgt instanceof Relation");
    	Relation l = (Relation) lft;
    	Relation r = (Relation) rgt;
        return Relation.union(Relation.join(l,r),Database.R00);
    }  
}

