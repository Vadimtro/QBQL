package qbql.lattice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.index.IndexedPredicate;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Token;
import qbql.util.Util;

public class Database {

    private Map<String,Predicate> lattice = new TreeMap<String,Predicate>();
    public Predicate getPredicate( String name ) {
    	Predicate ret = lattice.get(name);
    	if( ret == null ) { 
    		if( "R11".equals(name) ) {
    			buildR10();
    			return buildR11();
    		} else if( "R10".equals(name) )
    			return buildR10();
    	}
    	return ret;
    }
    public Predicate lookup( String name ) {
        Predicate ret = getPredicate(name);
        if( ret != null ) 
            return ret;
		name = name.startsWith("\"")&&name.endsWith("\"") ? name.substring(1, name.length()-1) : name;

    	List<LexerToken> src = new Lex().parse(name);
    	if( src.size() == 3 && "=".equals(src.get(1).content) ) {
    		LexerToken first = src.get(0);
            LexerToken second = src.get(2);
            if( first.type == Token.DIGITS ) {
                Relation relation = new Relation(new String[]{second.content});
                Integer i = Integer.parseInt(first.content);
                relation.addTuple(new Object[]{i});
                return relation;
            } else if( second.type == Token.DIGITS ) {
                Relation relation = new Relation(new String[]{first.content});
                Integer i = Integer.parseInt(second.content);
                relation.addTuple(new Object[]{i});
                return relation;
            } else
                return new EqualityPredicate(first.content, second.content);
    	}
        if( src.size() == 5 ) {  // select "20 <= age"^ "gender='female'" (Eats join Person);
                                 //                      ^^^^^^^^^^^^^^^
            int eqPos = -1;
            String attr = null;
            String value = null;
            boolean expectValue = false;
            for( int i = 0; i < src.size(); i++ ) {
                LexerToken t = src.get(i);
                if( "=".equals(t.content) ) {
                    eqPos = i;
                    continue;
                }
                if( "'".equals(t.content) ) {
                    if( value == null )
                        expectValue = true;
                    else
                        expectValue = false;
                    continue;
                }
                if( t.type == Token.IDENTIFIER ) {
                    if( expectValue )
                        value = t.content;
                    else
                        attr = t.content;
                    continue;
                }                    
            }
            if( 0 < eqPos && value != null ) {
                Relation relation = new Relation(new String[]{attr});
                relation.addTuple(new Object[]{value});
                return relation;
            }
        }
        try {
            Set<String> ints = new HashSet<String>();
            for( LexerToken t : src ) {
                if( t.type == Token.DIGITS ) 
                    ints.add(t.content);
            }
            if( ints.size() == 0 )
                return new IndexedPredicate(this,name);
            Relation rel = new Relation(ints.toArray(new String[]{}));
            Map<String, Object> body = new HashMap<String, Object>(); 
            for( String s : ints ) {
                Integer t = null;
                try {
                    t = Integer.parseInt(s);
                } catch( NumberFormatException e ) {}
                body.put(s,t==null?s:t);
            }
            rel.addTuple(body);
            return Predicate.setIX(new IndexedPredicate(this,name), rel);
        } catch ( Exception e ) {
            for( String qName : predicateNames() ) {
            	if( !qName.startsWith("\"") )
            		continue;
            	String candidate = qName.substring(1,qName.length()-1);
            	Map<String,String> matched = Predicate.matchNames(candidate, name);
            	if( matched == null )
            		continue;
            	ret = getPredicate(qName).clone();
            	for( String key : matched.keySet() ) {
            		String val = matched.get(key);
            		if( !val.equals(key) )
            			ret.renameInPlace(key, val);
            	}
            	return ret;
            }
            return null;
        }
    }

    public void addPredicate( String name, Predicate relvar ) {
        lattice.put(name, relvar);
    }       
    public void removePredicate( String name ) {
        lattice.remove(name);
    }       
    public String[] relNames() {
        Set<String> ret = new TreeSet<String>();
        for( String pred : lattice.keySet() )
            if( lattice.get(pred) instanceof Relation )
                ret.add(pred);
        return ret.toArray(new String[0]);
    }  
    public Set<String> predicateNames() {
        return lattice.keySet();
    }  
    
    private Map<String,Expr> newOperations = new TreeMap<String,Expr>();
    public Expr getOperation( String name ) {
    	Expr ret = newOperations.get(name);
    	if( ret == null )
    		throw new AssertionError("Operation '"+name+"' definition missing");
    	return ret;
    }
    public void addOperation( String name, Expr expr ) {
    	newOperations.put(name, expr);
    }       
    public Set<String> operationNames() {
    	return newOperations.keySet();
    }   
	public void restoreOperations( Set<String> target ) {
		if( newOperations.keySet().size() == target.size() )
			return;
		String extra = null;
		for( String key : newOperations.keySet() )
			if( !target.contains(key) ) {
				extra = key;
				break;
			}
		newOperations.remove(extra);		
		if( newOperations.keySet().size() != target.size() )
			throw new AssertionError("Only one extra operation is allowed");
	}
   
    public static Relation R00 = new Relation(new String[]{});
    private Relation R11;
    public static Relation R01 = new Relation(new String[]{});
    private Predicate R10;

    static {
        R01.addTuple(new TreeMap<String,Object>());
    }

    public String pkg = null;    
    public Database( String pkg ) {                         
        addPredicate("R00",Database.R00); 
        addPredicate("R01",Database.R01);
        this.pkg = pkg;
    }
    

    /**
     * Generalized set intersection and set union
     */
    Relation quantifier( Relation x, Relation y, int type )  {
    	if( type == Program.setIX )
    		throw new AssertionError("Wrong method for calcualting set intersection join");
    	
    	if( R11 == null ) {
    		buildR10();
    		buildR11();
    	}
    	
        Set<String> headerXmY = new TreeSet<String>();
        headerXmY.addAll(x.header.keySet());
        headerXmY.removeAll(y.header.keySet());            
        Set<String> headerYmX = new TreeSet<String>();
        headerYmX.addAll(y.header.keySet());
        headerYmX.removeAll(x.header.keySet());
        Set<String> headerSymDiff = new TreeSet<String>();
        headerSymDiff.addAll(headerXmY);
        headerSymDiff.addAll(headerYmX);
        Relation hdrXmY = new Relation(headerXmY.toArray(new String[0]));
        Relation hdrYmX = new Relation(headerYmX.toArray(new String[0]));
        
        Relation ret = new Relation(headerSymDiff.toArray(new String[0]));
                
        boolean isCoveredByR11 = R10.header.keySet().containsAll(x.header.keySet())
                              && Relation.le(x, Relation.union(R11,x));
        Relation X = isCoveredByR11 ? Relation.union(R11,hdrXmY) : Relation.union(x,hdrXmY);
        isCoveredByR11 = R10.header.keySet().containsAll(y.header.keySet())
                      && Relation.le(y, Relation.union(R11,y));
        Relation Y = isCoveredByR11 ? Relation.union(R11,hdrYmX) : Relation.union(y,hdrYmX);
        // (if R11 is wrong then calculate domain independent part of set join)  
        
        Relation hdrYX = Relation.union(Relation.join(R00,x),Relation.join(R00,y));
        for( Tuple xi : X.getContent() ) {
            Relation singleX = new Relation(X.colNames);
            singleX.addTuple(xi.data);
            Relation lft = Relation.union(Relation.join(singleX,x),hdrYX); 
            for( Tuple yi : Y.getContent() ) {
                Relation singleY = new Relation(Y.colNames);
                singleY.addTuple(yi.data);
                Relation rgt = Relation.union(Relation.join(singleY,y),hdrYX);
                if( type == Program.contains && Relation.le(lft, rgt) 
                 || type == Program.transpCont && Relation.ge(lft, rgt)   
                 || type == Program.disjoint && Predicate.le(lft, Predicate.join(Predicate.union(R11,rgt),complement(rgt))) 
                 || type == Program.almostDisj && Relation.join(lft, rgt).getContent().size()==1 
                 || type == Program.big && Predicate.ge(lft, Predicate.join(Predicate.union(R11,rgt),complement(rgt)))   
                 || type == Program.setEQ && lft.equals(rgt)
                )
                    ret = Relation.union(ret, Relation.join(singleX, singleY));
                /*if( type == Grammar.unison && lft.equals(rgt) ) {
                    if( x.colNames.length != y.colNames.length )
                        return ret;

                    ret = Relation.union(ret, Relation.join(singleX, singleY));
                }*/
            }
        }        
        return ret;
    }
    
    Map<String,Boolean> finiteDomains = new HashMap<String,Boolean>();
    /**
     * Complement x' returns relation with the same header as x
     * with tuples which are not in x
     * Axioms:
        x' ^ x = x ^ R00.
        x' v x = x v R11.
     */
    Predicate complement( Predicate x ) {
        return new ComplementPredicate(x);
    }

    /**
     * Inverse x` 
     * Axioms:
        x` ^ x = x ^ R11.
        x` v x = x v R00. 
    */ 
    Predicate inverse( Predicate x ) {
        return new InversePredicate(x);
    }
    
    private Predicate buildR10() {
        Predicate ret = new Relation(new String[]{});
        for( Predicate rel : lattice.values() )
            if( rel instanceof Relation )
            	ret = Predicate.join(ret, rel);
        R10 = ret;
        lattice.put("R10",ret);
        return ret;
    }

    private Predicate buildR11() {
        Map<String, Relation> domains = new HashMap<String, Relation>();
        for( String col : R10.colNames )
            domains.put(col, new Relation(new String[]{col}));

        for( Predicate rel : lattice.values() )
            if( rel instanceof Relation )
                for( Tuple t : ((Relation)rel).getContent() )
                    for( int i = 0; i < t.data.length; i++ ) {
                        domains.get(rel.colNames[i]).addTuple(new Object[]{t.data[i]});
                    }

        /* Bad performance for large db
        Relation ret = R01;
        for( Relation domain : domains.values() )
            ret = Relation.join(ret, domain);
        return ret;
        */
        
        Map<String, Object[]> doms = new TreeMap<String, Object[]>();
        for( String r : domains.keySet() ) {
            Set<Tuple> tuples = domains.get(r).getContent();
            Object[] content = new Object[tuples.size()];
            int i = 0;
            for( Tuple t : tuples )
                content[i++] = t.data[0];
            doms.put(r, content);
        }
        
        Relation ret = new Relation(doms.keySet().toArray(new String[0]));
        
        try {
            Map<String, Integer> indexes = new HashMap<String, Integer>();
            for (String key: doms.keySet())
                indexes.put(key, 0);
            do {
                Object[] t = new Object[ ret.colNames.length ];
                for (String key: doms.keySet())
                    t[ ret.header.get(key) ] = doms.get(key)[ indexes.get(key) ];

                ret.addTuple(t);

            } while (next(indexes, doms));
        } catch( Exception e ) { // for empty domains
        }        
        R11 = ret;
        lattice.put("R11",ret);
        
        return ret;
    }    
    static boolean next( Map<String, Integer> state, Map<String, Object[]> doms ) {
        for( String pos: state.keySet() ) {
            int rownum = state.get(pos);
            if( rownum < doms.get(pos).length-1 ) {
                state.put(pos, rownum+1);
                return true;
            }
            state.put(pos, 0);
        }
        return false;
    }
    
    public boolean equivalent( Relation x, Relation y ) {
        if( x == y )
            return true;
        if( x.getContent().size() != y.getContent().size() )
            return false;
        
        if( !submissive(x,y) )
            return false;
        return submissive(y,x);
    } 
    
    public boolean submissive( Relation x, Relation y ) {
        
        if( y.colNames.length == 0 ) { // indexing is broken this case
            if( y.getContent().size() == 0  )
                return x.getContent().size() == 0;
            else
                return Relation.union(x,getPredicate("R11")).equals(x);
        }
            
        int[] indexes = new int[x.colNames.length];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
        do {
            boolean matchedAllRows = true;
            for ( Tuple tx: x.getContent() ) { 
                boolean matchedX = false;
                for ( Tuple ty: y.getContent() ) {
                    boolean mismatchedField = false;
                    for( int i = 0; i < indexes.length; i++ ) {
                        if( !tx.data[i].equals(ty.data[indexes[i]]) ) {
                            mismatchedField = true;
                            break;
                        }
                    }
                    if( mismatchedField )
                        continue;
                    matchedX = true;
                    break;
                }
                if( !matchedX ) {
                    matchedAllRows = false;
                    break;
                }
            }
            if( matchedAllRows )
                return true;
        } while ( Util.next(indexes,y.colNames.length) );

        return false;
    }
        
    public Relation unnamedJoin( Relation x, Relation y ) {
        
        class Candidate {
            int[] indexes;
            Relation ret;
            public Candidate( Relation x, Relation y ) {
                indexes = new int[y.colNames.length];
                for( int i = 0; i < indexes.length; i++ )
                    indexes[i] = -1;
                ret = Relation.join(x,Relation.renameCols(x, y, indexes));
            }
            private Candidate( int[] indexes, Relation ret ) {
                this.indexes = indexes;
                this.ret = ret;
            }
            void spawn( Relation x, Relation y, int i, int j, 
                        List<Candidate> additions, List<Candidate> removals ) {
                int[] incr = new int[indexes.length];
                for( int k = 0; k < incr.length; k++ ) {
                    incr[k] = indexes[k];
                }
                incr[j] = i;
                Relation join = Relation.join(x,Relation.renameCols(x, y, incr));
                if( submissive(join,ret) 
                 && submissive(x,join) 
                 && submissive(y,join) 
                ) { 
                    boolean exists = false;
                    for( Candidate c: additions ) {
                        if( c.ret.equals(join) )
                            exists = true;
                    }
                    if( !exists )
                        additions.add( new Candidate(incr,join));
                    exists = false;
                    for( Candidate c: removals ) {
                        if( c.ret.equals(ret) )
                            exists = true;
                    }
                    if( !exists )
                        removals.add(this);
                }               
            }
        }
        
        //if( x.equals(R11) && y.equals(R11) ) // performance shortcut
            //return R11;
        
        List<Candidate> candidates = new LinkedList<Candidate>();
        candidates.add(new Candidate(x,y));

        for( int j = 0; j < y.colNames.length; j++ ) {
//System.out.println("j="+j);
            List<Candidate> additions = new LinkedList<Candidate>();
            List<Candidate> removals = new LinkedList<Candidate>();
            for( int i = 0; i < x.colNames.length; i++ ) {
//System.out.println("i="+i);
//if( j==2 && i==0 )
//System.out.println("*****");    
                if( !Relation.match(x,y,i,j) )
                    continue;
                for( Candidate src : candidates ) 
                    src.spawn(x, y, i, j, additions, removals);
            }
            candidates.removeAll(removals);
            candidates.addAll(additions); 
        }
        
        Relation ret = null;
        for( Candidate c : candidates )
            if( ret == null || submissive(c.ret, ret) )
                ret = c.ret;
        return ret;
    }

    public static Relation unnamedMeet( Relation x, Relation y ) {
        throw new RuntimeException("Not impl");
    }
    
    private Map<String, Integer> columnEqClasses = null;
    public Predicate EQclosure( Predicate rel ) {
        if( !(rel instanceof Relation) )
            throw new AssertionError("!(rel instanceof Relation)");
        if( columnEqClasses == null ) {
            columnEqClasses = new HashMap<String, Integer>();
            int cnt = 0;
            for( String col : getPredicate("R11").colNames ) {
                Relation colRel = Relation.union(R11, new Relation(new String[]{col}));
                String match = null;
                for( String candidate : columnEqClasses.keySet() ) {
                    Relation candidatelRel = Relation.union(R11, new Relation(new String[]{candidate}));
                    if( colRel.content.size() != candidatelRel.content.size() )
                        continue;
                    candidatelRel.renameInPlace(candidate, col);
                    Predicate cmp = quantifier(candidatelRel, colRel,Program.setEQ);
                    if( cmp.equals(R01) ) {
                        match = candidate;
                        break;
                    }
                }
                if( match != null )
                    columnEqClasses.put(col, columnEqClasses.get(match));
                else
                    columnEqClasses.put(col, cnt++);                    
            }
        }
        
        Predicate ret = rel;
        for( String col : rel.colNames ) {
            Integer i = columnEqClasses.get(col);
            for( String eqCol: columnEqClasses.keySet() ) {
                if( eqCol.equals(col) )
                    continue;
                if( i == columnEqClasses.get(eqCol) )
                    ret = Relation.join(ret, new EqualityPredicate(col,eqCol));
            }
            
        }
        
        return ret;
    }
    
}
