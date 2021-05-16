package qbql.lattice;

import java.util.HashSet;
import java.util.Set;

public class InversePredicate extends Predicate {
	Predicate src = null;

    public InversePredicate( Predicate src ) {
        super(src.colNames);
        this.src = src;
    }
    
    public static Relation union( Relation x, InversePredicate y )  {
        if( !(y.src instanceof Relation ))
            throw new AssertionError("!(y.src instanceof Relation )");
    	
    	Set<String> header = new HashSet<String>();
    	header.addAll(x.header.keySet());
    	header.removeAll(y.header.keySet());
    	Relation tmp = new Relation(header.toArray(new String[0]));
    	for( Tuple ty : ((Relation)y.src).getContent() ) {
    		boolean foundMatch = false;
    		for( Tuple tx : x.getContent() )
    			if( tx.matches(ty, x, (Relation)y.src) )
    				foundMatch = true;
    		if( !foundMatch )
                throw new AssertionError("Unsafe union with inversion");
    			
    	}
    	return Relation.union(x, tmp);
    }
    
    public void renameInPlace( String from, String to ) {
    	super.renameInPlace(from, to);
    	src.renameInPlace(from, to);
    }
    
    protected InversePredicate clone() {
        return new InversePredicate(src);
    }
}
