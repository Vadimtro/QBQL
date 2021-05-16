package qbql.lang;

import qbql.lattice.Database;
import qbql.lattice.Relation;

public class LT {
    public static String[] getSymbolicNames() {
        return new String[] {
            "lft < rgt", 
            "rgt > lft"
        };
    }

    public static Relation lft_rgt( Object lft, Object rgt ) {
        if( rgt.getClass().equals(lft.getClass()) ) {
            if( lft instanceof Comparable ) {
                Comparable l = (Comparable) lft;
                Comparable r = (Comparable) rgt;
                if( l.compareTo(r)<0 )
                    return Database.R01;
                else
                    return Database.R00;
            }
        }
        if( !(lft instanceof Number) || !(rgt instanceof Number) )
            throw new AssertionError("LT !lft/rgt instanceof Number");
        Double l = ((Number)lft).doubleValue();
        Double r = ((Number)rgt).doubleValue();
        if( l.compareTo(r)<0 )
            return Database.R01;
        else
            return Database.R00;
    } 
    
}
