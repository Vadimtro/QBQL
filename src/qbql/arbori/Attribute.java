package qbql.arbori;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.arbori.AncestorDescendantNodes.Type;
import qbql.parser.ParseNode;

abstract class Attribute {
	String name;
    
	static ParseNode getSibling( ParseNode self, ParseNode parent, int offset ) {
		//faster
		if( offset == 1 ) {
			boolean matched = false;
			for( ParseNode c : parent.children() ) {
				if( matched )
					return c;
				if( c == self )
					matched = true;
			}
			return null;
		}
		if( offset == -1 ) {
			ParseNode prior = null;
			for( ParseNode c : parent.children() ) {
				if( c == self )
					return prior;
				prior = c;
			}
			return null;
		}
		// TODO: optimize 
		final ParseNode[] children = parent.children().toArray(new ParseNode[] {});
		for( int i = 0; i < children.length; i++ ) {
			ParseNode child = children[i];
			if( child == self ) {
				int pos = i +offset;
				if( 0 <= pos && pos < children.length )
					return children[pos];
			}
		}
		return null;
	}
	
	protected static String referredTo( String attr ) {
        int pos = attr.indexOf('<');
        if( 0 < pos ) {
            String postfix = attr.substring(pos+1).trim();
            if( postfix.charAt(0)=='=' || postfix.charAt(0)=='<' )
                postfix = postfix.substring(1);
            if( postfix.indexOf('.') < 0 
              & postfix.indexOf(']') < 0 
            )  return postfix; // node descendant in ancestor-descendant constraint
        }
	    pos = attr.indexOf('=');
        if( 0 < pos )
            return attr.substring(pos+1);
        else if( attr.endsWith("^") )
	        return attr.substring(0,attr.length()-1);
	    else {//if( attr.endsWith("+1") || attr.endsWith("-1") )
	         //return attr.substring(0,attr.length()-2);
			int pos1 = attr.lastIndexOf('-');
			int pos2 = attr.lastIndexOf('+');
			if( pos1 < pos2 ) 
				pos1 = pos2;
			if( 0 < pos1 )
				try {
					String suffix = attr.substring(pos1);
					final int n = Integer.parseInt(suffix);
					return attr.substring(0,pos1);
				} catch( NumberFormatException e ) {}
	    }
	    pos = attr.lastIndexOf('.');
	    if( 0 < pos )
            return attr.substring(0,pos);
	        //return attr.substring(pos+1);
	    return null;
	}
    boolean isDependent( String primaryVar, Map<String,Attribute> varDefs ) {
        if( primaryVar.equals(name) )
            return true;
        Attribute ref = referredTo(varDefs);
        if( ref == null )
            return false;
        return ref.isDependent(primaryVar, varDefs);
    }
	
	@Override
	public String toString() {
		return name;
	}
	
    Predicate appendProposition( Predicate extra, Predicate p ) {
        return new CompositeExpr(extra, p, Oper.CONJUNCTION);
    }
    static Predicate unaryFilter( Predicate p, String nodeVar ) {
        if( p instanceof NodeContent ) {
            NodeContent nc = (NodeContent)p;
            if( !nc.nodeVar.equals(nodeVar) )
                return new True();
            else
            	return p;
        }
        if( p instanceof PositionalRelation ) {
            PositionalRelation pr = (PositionalRelation)p;
            if( pr.a instanceof BindVar && pr.b.name.equals(nodeVar) ) return p;
            else if(  pr.b instanceof BindVar && pr.a.name.equals(nodeVar) ) return p;
            else if(  pr.a.name.equals(nodeVar) && pr.b.name.equals(nodeVar) ) return p;
            else
            	return new True();
        }
        if( p instanceof NodeMatchingSrc ) {
            NodeMatchingSrc nms = (NodeMatchingSrc)p;
            if( !nms.nodeVar.equals(nodeVar) )
            	return new True();
            else
            	return p;
        }
    	
        if( p instanceof CompositeExpr ) {
            CompositeExpr ce = (CompositeExpr)p;
            if( ce.oper == Oper.CONJUNCTION || ce.oper == Oper.DISJUNCTION ) {
                Predicate lft = unaryFilter(ce.lft,nodeVar);
                Predicate rgt = unaryFilter(ce.rgt,nodeVar);
                return new CompositeExpr(lft,rgt,ce.oper);
             } else if( ce.oper == Oper.NEGATION ) {
            	return new True();
             } 
        } 
        return new True();
    }

	
	/**
	 * Evaluate attribute (fill in null values) in nodeAssignments
	 * If Attribute is in many-to-one, then spawn nodeAssignments tuples
	 * @param nodeAssignments
	 * @param root
	 * @return  mutated list
	 */
	abstract Set<Tuple> eval(Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root); 
	
	/**
	 * This method chases attribute functional dependencies, until it finds an attribute listed in attributePositions
	 * Then, the value along dependency chain is calculated 
	 * @param attributePositions
	 * @param tuple
	 * @param varDefs
	 * @return
	 */
	abstract ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs);
	
	abstract Attribute referredTo( Map<String,Attribute> varDefs );
}


class EqualExpr extends Attribute {
    String def;
	public EqualExpr( String name, String def ) {
	    this.name = name;
        this.def = def;
	}
	@Override
	Set<Tuple> eval( Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root ) {
        int nameCol = attributePositions.get(name);
        int defCol = attributePositions.get(def);
	    for( Tuple t : candidates ) {
	        t.values[nameCol] = t.values[defCol]; 
        }
	    return candidates;
	}
    @Override
    Attribute referredTo(Map<String, Attribute> varDefs) {
        return varDefs.get(def);
    }
    @Override
    public String toString() {
        return '"'+name+'='+def+'"';
    }
	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		return referredTo(varDefs).lookup(attributePositions, tuple, varDefs);
	}

}

/****    
    * AncestorExpr assists evaluating Arbori query in a single walk of parse tree.
    * This is faster than evaluating Cartesian product, then filtering out all tuples 
    * which don't match a filter condition.
    * 
    * For example, a typical "ancestor < descendant" predicate can be evaluated via nested loops over all tree nodes
    * and keeping all the node pairs which match "ancestor < descendant" condition. This method has complexity N^2. 
    * 
    * A Better alternative is to walk the tree (complexity=N) and for each node traverse the ancestors path (complexity=log(N))
    * with total complexity is N*log(N). 
    * 
    * With cartesian product implementation Arbori evaluation time of descendantNodes query in format.prg 
    * for FND_STATS package body was 230 sec (12646 nodes, max tree depth = 73) 
    * With ancestor chain filtering the timing was expected to improve by the factor of 12646/73
    * Indeed, the measured timing now is:
isolatedNodes eval time = 258 (ms)
descendantNodes eval time = 718
selNodes eval time = 31
ancestors eval time = 0
descendants eval time = 0
hierarchy eval time = 1208
"built-ins" eval time = 81
"ids" eval time = 110
identifiers eval time = 30
formattedNodes eval time = 0
paddedIdsInScope eval time = 210
extraLines eval time = 10
notPaddedParenthesis eval time = 290
(aggregate) eval time = 2946
callback time = 460
*/
class AncestorExpr extends Attribute {
    String def;
    Type type;
    Predicate unaryFilter;
    public AncestorExpr( String name, String def, Type type, Predicate full ) {
        this.name = name;
        this.def = def;
        this.type = type;
        unaryFilter = unaryFilter(full,name);
    }
    @Override
    Set<Tuple> eval( Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root ) {
    	Set<Tuple> ret = new TreeSet<Tuple>();
        int nameCol = attributePositions.get(name);
        int defCol = attributePositions.get(def);
        for( Tuple t : candidates ) {
            ParseNode node = t.values[defCol];
            if( type == Type.CLOSEST && node != null ) // reflexive result for for closest ancestor with overlapping condition would have been confusing
                node = node.parent();
            while( node != null ) {
                ParseNode[] t1 = new ParseNode[t.values.length];
                System.arraycopy(t.values, 0, t1, 0, t.values.length );
                t1[nameCol] = node;
                if( unaryFilter.eval(attributePositions, t1, null/*src*/, null/*varDefs*/) ) // TODO: fix null bug
                	ret.add(new Tuple(t1));
                node = node.parent();
            }             
        }
        //System.out.println(name+"="+ret.size());
        return ret;
    }
    @Override
    Attribute referredTo(Map<String, Attribute> varDefs) {
        return varDefs.get(def);
    }
    @Override
    public String toString() {
        return '"'+name+type.oper()+def+'"';
    }
	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		//throw new AssertionError("Not a functinally dependent attribute");
		Integer pos = attributePositions.get(name);
		return tuple[pos];
	}
}


	
class Parent extends Attribute {	
    String ref;
	public Parent( String name ) {
		this.name = name;
		ref = referredTo(name);
	}
    @Override
    Set<Tuple> eval(Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root) {
        int nameCol = attributePositions.get(name);
        int refCol = attributePositions.get(ref);
        for( Tuple t : candidates ) {
            if( t.values[refCol] == null )
                continue;
            t.values[nameCol] = t.values[refCol].parent(); 
        }
        return candidates;
    }
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(ref);
    }
	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		return referredTo(varDefs).lookup(attributePositions, tuple, varDefs).parent();
	}
}

class Sibling extends Attribute {	
    String ref;
    int shift;   // successor
	public Sibling( String name, int shift ) {
		this.name = name;
        ref = referredTo(name);
        this.shift = shift;
	}
    @Override
    Set<Tuple> eval(Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root) {
        int nameCol = attributePositions.get(name);
        int refCol = attributePositions.get(ref);
        for( Tuple t : candidates ) {
            if( t.values[refCol] == null )
                continue;
            t.values[nameCol] = t.values[refCol].parent(); 
            if( t.values[nameCol] == null )
                continue;
            t.values[nameCol] = getSibling(t.values[refCol], t.values[nameCol], shift);
        }
        return candidates;
    }
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(ref);
    }
	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		ParseNode self = referredTo(varDefs).lookup(attributePositions, tuple, varDefs);
		return getSibling(self,self.parent(),shift);
	}
}


class Column extends Attribute {
    String rel;
    public Column( String name ) {
        int pos = name.indexOf('.');
        //this.name = name.substring(pos+1);
        this.name = name;
        rel = name.substring(0,pos);
    }
    @Override
    Set<Tuple> eval(Map<String, Integer> attributePositions, Set<Tuple> candidates, ParseNode root) {
        int nameCol = attributePositions.get(name);
        int defCol = attributePositions.get(rel);
        for( Tuple t : candidates ) {
            t.values[nameCol] = t.values[defCol]; 
        }
        return candidates;
    }
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(rel);
    }
    @Override
    boolean isDependent( String primaryVar, Map<String,Attribute> varDefs ) {
        if( primaryVar.equals(name) )
            return true;
        return super.isDependent(primaryVar, varDefs);
    }
	@Override
	ParseNode lookup(Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs) {
		Integer pos = attributePositions.get(name);
		if( pos == null )
			throw new AssertionError("Missing column "+name);
		return tuple[pos];
	}

    /*@Override
    public String toString() {
        return rel+'.'+name;
    }*/
}





	

