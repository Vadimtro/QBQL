package qbql.arbori;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.arbori.AncestorDescendantNodes.Type;
import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.util.Util;

/**
 * View table as column store database
 * Evaluate all unary predicates first
 * IndependentAttribute is such an unary relation
 * @author Dim
 *
 */
public class IndependentAttribute extends Attribute {
    private MaterializedPredicate content;
    public IndependentAttribute( String name ) {
        this.name = name;
    }
    
    public MaterializedPredicate getContent(){
        return content;
    }
    public void setContent( MaterializedPredicate src ){
        content = src;
    }
    
    /**
     * Initialize an "unary" relation (with independent attributes and all its dependences)
     * @param root
     * @param src
     * @param varDefs
     * @param predVar
     */
    public void initContent( ParseNode root, List<LexerToken> src, AttributeDefinitions varDefs, String predVar ) {
        ArrayList<String> attributes = new ArrayList<String>();
        //ArrayList<String> header = new ArrayList<String>();
        attributes.add(name);
        //header.add(name);
        
        for( String candidate : varDefs.keySet() ) {
            Attribute attr = varDefs.get(candidate);
            if( attr == null )
                attr = null;
            if( !attributes.contains(candidate) && attr.isDependent(name, varDefs) ) {
                attributes.add(candidate);
                //if( signature.contains(candidate) )
                    //header.add(candidate);
            }
        }
                
        content = new MaterializedPredicate(attributes/*header*/, src, null);
        Predicate fullPredicate = varDefs.namedPredicates.get(predVar);
                
        OUTER: for( ParseNode node =root; node != null;  node = node.next() ) {
            Set<Tuple> candidates = new TreeSet<Tuple>();
            ParseNode[] t = new ParseNode[content.arity()];
            candidates.add(new Tuple(t));
            t[content.getAttribute(name)] = node;
            if( !unaryFilter(fullPredicate,name).eval(content.attributePositions, t, src, varDefs) )
            	continue OUTER;
            for( String attr : attributes ) {
                candidates = content.assignDependencyChain(attr,candidates,varDefs,root);
                if( candidates.size() == 0 )
                    continue OUTER;
            }
            
            candidates = filterUnaryPredicates(varDefs,predVar,content.attributePositions,candidates,root,src);
            
            for( Tuple tmp : candidates ) 
                content.tuples.add(tmp);                            
        }
        
        content.tuples = filterClosestAncestorDescendants(varDefs,predVar,content.attributePositions,content.tuples,root,src);
    }

    int getLimits() {
        return content.cardinality();
    }
    
    // optimization 1
    private Predicate failfastFilter = null;
    void putFilter( Predicate filter ) {
        failfastFilter = filter;        
    }
    
    private Set<Tuple> filterClosestAncestorDescendants( Map<String, Attribute> varDefs, String predVar, Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root, List<LexerToken> src ) {
    	if( candidates.size() < 2 )
    		return candidates;
    	
    	// TODO: can handle only chain of 2 ancestor-descendant, e.g.
    	//   & selClause < longCaseExpr
    	//   & longCaseExpr < elseOrCase
    	int iDesc = -1;
		int iAnc = -1;
		AncestorExpr prior = null;
    	for( String a : varDefs.keySet() ) {
    		Attribute attr = varDefs.get(a);
    		if( ! (attr instanceof AncestorExpr) )
    			continue;
    		AncestorExpr ancExpr = (AncestorExpr) attr;
     		if( ancExpr.type != Type.CLOSEST )
    			continue;
            if( !attr.isDependent(name, varDefs) ) 
            	continue;
            if( prior == null ) {
 				iDesc = attributePositions.get(ancExpr.def);
 				iAnc = attributePositions.get(ancExpr.name);
 				prior = ancExpr;
            } else {
            	if( prior.name.equals(ancExpr.def) ) {
            		iAnc = attributePositions.get(ancExpr.name);
            	} else if( prior.def.equals(ancExpr.name) ) {
            		iDesc = attributePositions.get(ancExpr.def);
            	}            	
            	break;
            }     		
    	}
    	if( iDesc != -1 && iAnc != -1 ) {
    		candidates =  filterNested(candidates, iAnc, iDesc, iDesc);
    		candidates =  filterNested(candidates, iDesc, iAnc, iDesc);
    	}
	    	
        return candidates;
    }

	private Set<Tuple> filterNested( Set<Tuple> candidates, int colA, int colB, int colC ) {
		HashMap<Long, Set<Tuple>> map = new HashMap<Long, Set<Tuple>>();   // ancestor pos -> descendents
		for( Tuple t : candidates ) {
			ParseNode anc = t.values[colA];
			
			long pos = Util.lPair(anc.from, anc.to);
			Set<Tuple> tuples = map.get(pos);
			if( tuples ==null ) {
				tuples = new TreeSet<Tuple>();
				tuples.add(t);
				map.put(pos, tuples);
				continue;
			}
			ParseNode desc = t.values[colB];
			Set<Tuple> delete = new TreeSet<Tuple>();
			for( Tuple tuple : tuples ) {
				ParseNode desc2 = tuple.values[colC];
				if( desc2 == desc )
					continue;
				if( desc.from <= desc2.from && desc2.to <= desc.to ) {
					delete.add(tuple);
				}
				if( desc2.from <= desc.from && desc.to <= desc2.to ) {
					delete = null;
					t = null;
					break;
				}
			}
			if( t != null )
				tuples.add(t);
			if( delete != null )
				tuples.removeAll(delete);
		}
		Set<Tuple> ret = new TreeSet<Tuple>();
 		for( Set<Tuple> ts : map.values() )
 			ret.addAll(ts); 
		return ret;
	}   
    
    private Set<Tuple> filterUnaryPredicates( AttributeDefinitions varDefs, String predVar, Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root, List<LexerToken> src ) {
        if( failfastFilter == null ) {
            Predicate fullPredicate = varDefs.namedPredicates.get(predVar);
            failfastFilter = unaryFilter(varDefs, fullPredicate);
        }
        Set<Tuple> ret = new TreeSet<Tuple>();
        int i = -1;
        for( Tuple t: candidates ) {
        	i++;
        	if( 0 == i%1000 )
           		if( Thread.interrupted() )
        			throw new AssertionError("Interrupted");
            if( failfastFilter.eval(attributePositions, t.values, src, varDefs) )
                ret.add(t);
        }
        return ret;
    }   
    
    /**
     * Unary predicate extract (for all attrinbutes)
     * @param varDefs
     * @param fullPredicate
     * @return
     */
    private Predicate unaryFilter( Map<String, Attribute> varDefs, Predicate fullPredicate ) {
        List<Predicate> tmp = new LinkedList<Predicate>();
        for( String s : varDefs.keySet() ) {
            Attribute attr = varDefs.get(s);
            if( attr.isDependent(name, varDefs) )
                tmp.add(unaryFilter(fullPredicate, attr.name));
        }
        
        Predicate ret = new True();
        for( Predicate extra : tmp )
            ret = appendProposition(extra, ret);
        return ret;
    }


    @Override
    Set<Tuple> eval(Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root) {
        throw new AssertionError("or should just return candidates?");
    }

    @Override
    Attribute referredTo(Map<String, Attribute> varDefs) {
        return null;
    }

	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		Integer pos = attributePositions.get(name);
		if( pos == null )
			throw new AssertionError("Missing column "+name);
		return tuple[pos];
		//throw new AssertionError("independent attribute?");
	}   
    
}


