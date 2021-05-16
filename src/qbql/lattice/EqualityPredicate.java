package qbql.lattice;

import java.util.HashSet;
import java.util.Set;

import qbql.index.IndexedPredicate;

public class EqualityPredicate extends Predicate {
    private String[] colsX;
    private String[] colsY;
    public EqualityPredicate( String[] colsX, String[] colsY ) {
    	assertDisjointSets(colsX, colsY);
    	if( colsX.length != colsY.length )
    		throw new AssertionError("Skewed equality predicate");
    	//-----super-----
        colNames = new String[colsX.length+colsY.length];
        for( int i = 0; i < colsX.length; i++ ) 
        	colNames[i] = colsX[i];
        for( int i = 0; i < colsY.length; i++ ) 
        	colNames[colsX.length+i] = colsY[i];
        for( int i = 0; i < colNames.length; i++ ) 
            header.put(colNames[i],i);                       
        //----------------
        this.colsX = colsX;
        this.colsY = colsY;
    }
    public EqualityPredicate( String colX, String colY ) {
        this(new String[]{colX}, new String[]{colY});
    }
    
    static Predicate setIX( Predicate x, EqualityPredicate y )   {
    	if( x instanceof EqualityPredicate ) {
    		EqualityPredicate eq = (EqualityPredicate)x;
    		String[] cX = new String[eq.colsX.length+y.colsX.length];
    		for( int i = 0; i < eq.colsX.length; i++ ) 
    			cX[i] = eq.colsX[i];
    		for( int i = 0; i < y.colsX.length; i++ ) 
    			cX[eq.colsX.length+i] = y.colsX[i];
    		String[] cY = new String[eq.colsX.length+y.colsX.length];
    		for( int i = 0; i < eq.colsX.length; i++ ) 
    			cY[i] = eq.colsY[i];
    		for( int i = 0; i < y.colsY.length; i++ ) 
    			cY[eq.colsY.length+i] = y.colsY[i];
    		return new EqualityPredicate(cX,cY);
    	}

    	if( x instanceof Relation ) {
    		Relation ret = (Relation)Relation.join(x, Database.R01); // clone
    		for( int i = 0; i < y.colsX.length; i++ ) {
    			String yColX = y.colsX[i];
    			String yColY = y.colsY[i];			                       

    			if( x.header.containsKey(yColY) && x.header.containsKey(yColX) ) {
    				Set<String> header = new HashSet<String>();
    				header.addAll(x.header.keySet());
    				header.remove(yColX);
    				header.remove(yColY);
    				Relation tmp = new Relation(header.toArray(new String[0]));
    				return Predicate.union(Predicate.join(x, y),tmp);
    			}
    			if( x.header.containsKey(yColX) && !x.header.containsKey(yColY) )
    				ret.renameInPlace(yColX, yColY);
    			else if( x.header.containsKey(yColY) && !x.header.containsKey(yColX) )
    				ret.renameInPlace(yColY, yColX);
    			else
    				throw new AssertionError("Renaming columns misaligned with target relation");
    		}
    		return ret; 
    	} else if( x instanceof IndexedPredicate ) {
    		IndexedPredicate ret = new IndexedPredicate((IndexedPredicate)x);
    		for( int i = 0; i < y.colsX.length; i++ ) {
    			String yColX = y.colsX[i];
    			String yColY = y.colsY[i];			                       

    			if( x.header.containsKey(yColX) && !x.header.containsKey(yColY) )
    				ret.renameInPlace(yColX, yColY);
    			else if( x.header.containsKey(yColY) && !x.header.containsKey(yColX)  )
    				ret.renameInPlace(yColY, yColX);
    			else
    				throw new AssertionError("Renaming columns misaligned with target relation");
    		}
    		return ret;                                
    	} else if( x.lft != null ) {
    		Predicate ret = x.clone();
    		for( int i = 0; i < y.colsX.length; i++ ) {
    			String yColX = y.colsX[i];
    			String yColY = y.colsY[i];			                       
    			if( ret.header.containsKey(yColX) && !x.header.containsKey(yColY) ) {
    				ret.renameInPlace(yColX, yColY);
    			} else if( ret.header.containsKey(yColY) && !x.header.containsKey(yColX) )
    				ret.renameInPlace(yColY, yColX);
    			else
    				throw new AssertionError("Renaming columns misaligned with target relation");
    		}
    		return ret;
    	}

    	throw new AssertionError("Unexpected case");
    }
    
    static Predicate join( Predicate x, EqualityPredicate y )  {
    	if( x instanceof EqualityPredicate ) {
    		EqualityPredicate eq = (EqualityPredicate)x;
    	    String[] cX = new String[eq.colsX.length+y.colsX.length];
            for( int i = 0; i < eq.colsX.length; i++ ) 
            	cX[i] = eq.colsX[i];
            for( int i = 0; i < y.colsX.length; i++ ) 
            	cX[eq.colsX.length+i] = y.colsX[i];
    	    String[] cY = new String[eq.colsX.length+y.colsX.length];
            for( int i = 0; i < eq.colsX.length; i++ ) 
            	cY[i] = eq.colsY[i];
            for( int i = 0; i < y.colsY.length; i++ ) 
            	cY[eq.colsY.length+i] = y.colsY[i];
            return new EqualityPredicate(cX,cY);
    	}
    	
    	Predicate ret = x.clone();
    	for( int i = 0; i < y.colsX.length; i++ ) {
    		String yColX = y.colsX[i];
    		String yColY = y.colsY[i];			                       
    		if( ret.header.containsKey(yColX) && !x.header.containsKey(yColY) ) {
    			ret.eqInPlace(yColX, yColY);
    		} else if( ret.header.containsKey(yColY) && !x.header.containsKey(yColX) )
    			ret.eqInPlace(yColY, yColX);
    		else
    			throw new AssertionError("Renaming columns are disjoint with target relation");
    	}
        return ret;       
    }
    static Predicate join( Relation x, EqualityPredicate y )  {
    	Relation ret = Relation.join(x, Database.R01); // clone
		for( int i = 0; i < y.colsX.length; i++ ) {
			String yColX = y.colsX[i];
			String yColY = y.colsY[i];
			ret = join(ret, yColX, yColY);
		}
    	return ret;
    }
    static Relation join( Relation x, String yColX, String yColY )  {
        String colX = null;
        String colY = null;
        Set<String> hdrX = x.header.keySet();
        if( hdrX.contains(yColX) && !hdrX.contains(yColY) ) {
            colX = yColX;
            colY = yColY;
        } else if( hdrX.contains(yColY) && !hdrX.contains(yColX) ) {
            colX = yColY;
            colY = yColX;
        }
        if( colX == null || colY == null ) {
            if( hdrX.contains(yColX) && hdrX.contains(yColY) ) {
                colX = yColX;
                colY = yColY;
                String[] header = new String[x.colNames.length];
                System.arraycopy(x.colNames, 0, header, 0, x.colNames.length);
                Relation ret = new Relation(header);
                for( Tuple t : x.getContent() ) 
                    if( t.data[x.header.get(colX)].equals(t.data[x.header.get(colY)]) ) {
                        Object[] o = new Object[x.colNames.length];
                        System.arraycopy(t.data, 0, o, 0, x.colNames.length);
                        ret.addTuple(o);
                    }
                return ret;
            } 
            throw new AssertionError("Equality column doesn't match relation");
        }
        String[] header = new String[x.colNames.length+1];
        System.arraycopy(x.colNames, 0, header, 0, x.colNames.length);
        header[x.colNames.length] = colY;
        Relation ret = new Relation(header);
        for( Tuple t : x.getContent() ) {
            Object[] o = new Object[x.colNames.length+1];
            System.arraycopy(t.data, 0, o, 0, x.colNames.length);
            o[x.colNames.length] = t.data[x.header.get(colX)];
            ret.addTuple(o);
        }
        return ret;
    }
    
    void assertDisjointSets( String[] arg1, String[] arg2 ) {
    	for( String s1 : arg1 )
        	for( String s2 : arg2 )
        		if( s1.equals(s2) )
        			throw new AssertionError("assertDisjointSets: common element "+s1);
    	for( String s1 : arg1 )
        	for( String s2 : arg1 )
        		if( s1!=s2 && s1.equals(s2) )
        			throw new AssertionError("assertDisjointSets: repeated element "+s1);
    	for( String s1 : arg2 )
        	for( String s2 : arg2 )
        		if( s1!=s2 && s1.equals(s2) )
        			throw new AssertionError("assertDisjointSets: repeated element "+s1);
    }
    
    public void renameInPlace( String from, String to ) {
    	super.renameInPlace(from, to);
        for( int i = 0; i < colsX.length; i++ ) {
        	if( from.equals(colsX[i]) )
        		colsX[i] = to;
        	if( from.equals(colsY[i]) )
        		colsY[i] = to;
        }
    }


    protected EqualityPredicate clone() {
        return new EqualityPredicate(colsX,colsY);
    }
}
