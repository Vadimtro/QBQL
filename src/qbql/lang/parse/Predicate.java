package qbql.lang.parse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;

/**
 * Arbori program consists of statements, each evaluates boolean expression of predicates
 * Here we classify various predicates
 * except for MaterializedPredicate and AggragatePredicate which warrant separate attention
 * @author Dim
 *
 */
interface Predicate {
    static final String UNASSIGNED_VAR = "unassigned var: ";
    /**
     * Evaluate expressions over predicate variables such as join of two MaterializedPredicates
     * @param target
     * @return
     */
    MaterializedPredicate eval( Parsed target );
    
	public String toString( int depth );
	
	/**
	 * Evaluate nodeAssignments tuple against predicate expression
	 * @param nodeAssignments
	 * @param src
	 * @return
	 */
    boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src );
    
    /**
     * Gather variables in the predicate expression
     * @param ret -- collect stuff here
     * @param optimizeEqs TODO
     * @param optimizeEqs -- if encounted disjunction, then can't assume that any variable find around equality predicate is dependent anymore
     */
	public void variables( Set<String> ret, boolean optimizeEqs );
	
    /**
     * Optimization: get all dependencies 
     * Predicate is mapped to True if dependency is positive
     */
    public Map<String,Boolean> dependencies();
    
	/**
	 * When evaluating predicate expression need to find attribute which is joined to intermeditory result
	 * so that the result would not be cartesian product 
	 * @param var1
	 * @param var2
	 * @return if there is binary predicate expression relating var1 with var2
	 */
    Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs );
    
}

/**
 * Predicates in the expression defined elsewhere
 * e.g.  "proc scope" | "block scope" | "loop scope"
 * @author Dim
 *
 */
class PredRef implements Predicate {
    String name;
    Program program; // named predicates in the Program are mutating

    public PredRef( String name, Program program ) {
        this.name = name;
        this.program = program;
    }

    @Override
    public boolean eval( Map<String, ParseNode> nodeAssignments, List<LexerToken> src ) {
        Predicate ref = program.namedPredicates.get(name);
        if( ref == null )
            throw new AssertionError("Unreferenced predicate variable "+name);
        if( !(ref instanceof MaterializedPredicate) )
            throw new AssertionError("Unevaluated predicate variable "+name);
        return ref.eval(nodeAssignments, src);
    }

    @Override
    public String toString( int depth ) {
        return "->"+name;
    }

    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
        //Predicate ref = program.symbolicPredicates.get(name);
        Predicate ref = program.namedPredicates.get(name);
        ref.variables(ret, optimizeEqs);
    }

    @Override
    public MaterializedPredicate eval(Parsed target) {
        Predicate ref = program.namedPredicates.get(name);
        if( ref == null )
            throw new AssertionError("Unreferenced predicate variable "+name);
        if( !(ref instanceof MaterializedPredicate) )
            throw new AssertionError("Unevaluated predicate variable "+name);
        return ref.eval(target);
    }

    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }

    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        ret.put(name,true);
        return ret;
    }
    
}

abstract class IdentedPredicate implements Predicate {
    public String toString( int depth ) {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < depth ;i++ )
            sb.append("  ");  //$NON-NLS-1$
		sb.append(toString());
		sb.append('\n');
        return sb.toString();
	}	
	ParseNode getNode( String nodeVar, Map<String,ParseNode> nodeAssignments ) throws AssertionError {
		ParseNode node = nodeAssignments.get(nodeVar);
		if( node == null ) {
			if ( 0<nodeVar.indexOf("-1") || 0<nodeVar.indexOf("+1") || 0<nodeVar.indexOf("^") )
				return null;
			else
				throw new AssertionError(UNASSIGNED_VAR+nodeVar);
		}
		return node;
	}
	static void variables( String var, Set<String> ret ) {
		ret.add(var);
        if( 0 < var.indexOf('=') ) {
            String prefix = var.substring(0,var.indexOf('='));
            //if( Attribute.referredTo(prefix) != null )
                variables(prefix, ret);
            variables(var.substring(var.indexOf('=')+1), ret);
        } else if( var.endsWith("^") )
			variables(var.substring(0,var.length()-1), ret);
		else if( var.endsWith("-1") || var.endsWith("+1") )
			variables(var.substring(0,var.length()-2), ret);
	}
    @Override
    public MaterializedPredicate eval( Parsed target ) {
        return null;
    }
}

/**
 * One of the most often used proposition, asserting that a node variable content includes a certain grammar symbol
 */
class NodeContent extends IdentedPredicate {
	String nodeVar;
	int content;
	public NodeContent( String nodeVar, int content ) {
		this.nodeVar = nodeVar;
		this.content = content;
	}
    @Override
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
		ParseNode node = getNode(nodeVar, nodeAssignments);
		if( node == null )
		    return false;
		return node.contains(content);
	}
    @Override
	public String toString() {
		return "["+nodeVar+") "+content;
	}
    @Override
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar,ret);
	}
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = nodeVar.indexOf('.');
        if( 0 < pos  )
            ret.put(nodeVar.substring(0,pos),true);
        return ret;
    }
}

/**
 * Nodes with the same lexical content. Example: PL/SQL variable declaration and usage 
 */
class NodesWMatchingSrc extends IdentedPredicate {
	String nodeVar1;
	String nodeVar2;
	public NodesWMatchingSrc( String nodeVar1, String nodeVar2 ) {
		this.nodeVar1 = nodeVar1;
		this.nodeVar2 = nodeVar2;
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
		ParseNode node1 = getNode(nodeVar1, nodeAssignments);
		if( node1 == null )
			return false;
		ParseNode node2 = getNode(nodeVar2, nodeAssignments);
		if( node2 == null )
			return false;
		return node1.content(src).equals(node2.content(src));
	}
	public String toString() {
		return "?"+nodeVar1+" = ?"+nodeVar2;
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar1,ret);
		variables(nodeVar2,ret);
	}
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        Attribute attr1 = varDefs.get(nodeVar1);
        Attribute attr2 = varDefs.get(nodeVar2);
        if( attr1.isDependent(var1,varDefs)&&attr2.isDependent(var2,varDefs) 
         || attr2.isDependent(var1,varDefs)&&attr1.isDependent(var2,varDefs) )
            return this;
        else
            return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = nodeVar1.indexOf('.');
        if( 0 < pos  )
            ret.put(nodeVar1.substring(0,pos),true);
        pos = nodeVar2.indexOf('.');
        if( 0 < pos  )
            ret.put(nodeVar2.substring(0,pos),true);
        return ret;
    }
}

/**
 * Node matching specific literal
 */
class NodeMatchingSrc extends IdentedPredicate {
	String nodeVar;
	String literal;
	public NodeMatchingSrc( String nodeVar, String literal ) {
		this.nodeVar = nodeVar;
		if( literal.charAt(0)!='\'' )
			throw new AssertionError("expected string literal");
		this.literal = literal.substring(1,literal.length()-1);
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
		ParseNode node = getNode(nodeVar, nodeAssignments);
		if( node == null )
			return false;
		return node.content(src).equals(literal);
	}
	public String toString() {
		return "?"+nodeVar+" = '"+literal+"'";
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar,ret);
	}
     @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
     @Override
     public Map<String, Boolean> dependencies() {
         Map<String, Boolean> ret = new HashMap<String, Boolean>();
         int pos = nodeVar.indexOf('.');
         if( 0 < pos  )
             ret.put(nodeVar.substring(0,pos),true);
         return ret;
     }
}

/**
 * Ancestor-descendant relationship betweeen nodes. 
 * Assumed to be reflexive, even though arbori syntax (x<y) hints otherwise 
 */
class AncestorDescendantNodes extends IdentedPredicate {
	String a;
	String d;
	boolean isStrict; 
	public AncestorDescendantNodes(  String ancestor, String descendant, boolean isStrict ) {
		this.a = ancestor;
		this.d = descendant;
		this.isStrict = isStrict;
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
		ParseNode nodeA = getNode(a, nodeAssignments);
		if( nodeA == null )
			return false;
		ParseNode nodeD = getNode(d, nodeAssignments);
		if( nodeD == null )
			return false;
		if( isStrict )
		    return nodeA.from <= nodeD.from && nodeD.to <= nodeA.to 
		    && (nodeA.from != nodeD.from || nodeD.to != nodeA.to);
		else
		    return nodeA.from <= nodeD.from && nodeD.to <= nodeA.to;
	}
    @Override
	public String toString() {
        if( isStrict )
            return a+" < "+d;
        else
            return a+" <= "+d;
	}
    @Override
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(a,ret);
		variables(d,ret);
	}
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        Attribute attr1 = varDefs.get(a);
        Attribute attr2 = varDefs.get(d);
        if( attr1.isDependent(var1,varDefs)&&attr2.isDependent(var2,varDefs) 
         || attr2.isDependent(var1,varDefs)&&attr1.isDependent(var2,varDefs) )
            return this;
        else
            return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = a.indexOf('.');
        if( 0 < pos  )
            ret.put(a.substring(0,pos),true);
        pos = d.indexOf('.');
        if( 0 < pos  )
            ret.put(d.substring(0,pos),true);
        return ret;
    }
}

/**
 * Comparing head or tail of one node with the other (or bind var, for that matter).
 */
enum PosType {HEAD,TAIL,BINDVAR};
class PositionalRelation extends IdentedPredicate {
	String a;
	PosType tA;
	String b;
	PosType tB;	
	Program prog;
	boolean isReflexive;
	public PositionalRelation( String a, PosType tA, String b, PosType tB, boolean isReflectxive, Program prog ) {
		this.a = a;
		this.tA = tA;
		this.b = b;
		this.tB = tB;
        this.isReflexive = isReflectxive;
		this.prog = prog;
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
	    if( isReflexive )
	        return evalPos(nodeAssignments, tA, a) <= evalPos(nodeAssignments, tB, b);
	    else
	        return evalPos(nodeAssignments, tA, a) < evalPos(nodeAssignments, tB, b);
	}
	private int evalPos( Map<String, ParseNode> nodeAssignments, PosType typ, String name ) {
		if( typ == PosType.BINDVAR )
			try {
				Field f = prog.getClass().getDeclaredField(name);
				f.setAccessible(true);
				return f.getInt(prog);
			} catch( Exception e ) {
				throw new AssertionError("Bind var '"+name+"' not found: "+e.getMessage());
			}
		else {
			ParseNode nodeA = getNode(name, nodeAssignments);
			if( typ == PosType.HEAD )
				return nodeA.from;
			else if( typ == PosType.TAIL )
				return nodeA.to;
		}
		throw new AssertionError("Impossible Case");
	}
	
	public String toString() {
		return a+tA+" < "+b+tB;
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		if( tA != PosType.BINDVAR )
			variables(a,ret);
		if( tB != PosType.BINDVAR )
			variables(b,ret);
	}
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        if( tA == PosType.BINDVAR || tB == PosType.BINDVAR )
            return null;
        Attribute attr1 = varDefs.get(a);
        Attribute attr2 = varDefs.get(b);
        if( attr1.isDependent(var1,varDefs)&&attr2.isDependent(var2,varDefs) 
         || attr2.isDependent(var1,varDefs)&&attr1.isDependent(var2,varDefs) )
            return this;
        else
            return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = a.indexOf('.');
        if( 0 < pos  )
            ret.put(a.substring(0,pos),true);
        pos = b.indexOf('.');
        if( 0 < pos  )
            ret.put(b.substring(0,pos),true);
        return ret;
    }
}

/**
 * Often, one have node variable or expression, and would like to specify it matching the same node 
 * as the other variable or expression. 
 * For example, the predicate  x^ = y^  asserts that nodes x and y are siblings.
 * 
 * Optimization: x = expr qualifies x as dependent variable and trivializes the predicate 
 * (x is functionally dependent on vars in the expr)
 */
class SameNodes extends IdentedPredicate {
    private String a;
	private String b;
    private boolean isFunctional = false; 
	public SameNodes( String a, String b ) {
		this.a = a;
		this.b = b;
		String ref = Attribute.referredTo(a);
		if( ref == null ) { // independent variable
		    isFunctional = true;
		    return;
		}
		ref = Attribute.referredTo(b);
		if( ref == null ) { // independent variable
            isFunctional = true;
		    return;
		}
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
	    if( isFunctional )
	        return true;
		ParseNode nodeA = getNode(a, nodeAssignments);
		if( nodeA == null )
			return false;
		ParseNode nodeB = getNode(b, nodeAssignments);
		if( nodeB == null )
			return false;
		return nodeA.from==nodeB.from && nodeA.to==nodeB.to;
	}
	public String toString() {
		return a+" = "+b;
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
	    if( optimizeEqs )
	        variables(a+"="+b,ret);
	    else {
	        isFunctional = false;
	        variables(a,ret);
	        variables(b,ret);	        
	    }
	}
    
    @Override
    ParseNode getNode(String nodeVar, Map<String, ParseNode> nodeAssignments) throws AssertionError {
        try {
            return super.getNode(nodeVar, nodeAssignments);
        } catch( AssertionError e ) {
            if( e.getMessage().startsWith(UNASSIGNED_VAR) ) {
                if( a.contains("^") || a.contains("+1") || a.contains("-1") || 
                    b.contains("^") || b.contains("+1") || b.contains("-1") ) 
                    return null;
                
            }
            throw e;            
        }
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        Attribute attr1 = varDefs.get(a);
        Attribute attr2 = varDefs.get(b);
        if( attr1.isDependent(var1,varDefs)&&attr2.isDependent(var2,varDefs) 
         || attr2.isDependent(var1,varDefs)&&attr1.isDependent(var2,varDefs) )
            return this;
        else
            return null;
    }
   
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = a.indexOf('.');
        if( 0 < pos  )
            ret.put(a.substring(0,pos),true);
        pos = b.indexOf('.');
        if( 0 < pos  )
            ret.put(b.substring(0,pos),true);
        return ret;
    }

}


/**
 * Composite expression build of primitive relations by operations of conjunction, disjunction, and negation
 * DIFFERENCE is not implemented yet
 */
enum Oper { CONJUNCTION, DISJUNCTION, NEGATION, DIFFERENCE };
class CompositeExpr implements Predicate {
	Predicate lft;
	Predicate rgt;
	Oper oper;
	public CompositeExpr( Predicate lft, Predicate rgt, Oper oper ) {
		this.lft = lft;
		this.rgt = rgt;
		this.oper = oper;
	}
	public boolean eval( Map<String,ParseNode> nodeAssignments, List<LexerToken> src ) {
		if( oper == Oper.CONJUNCTION )
			return lft.eval(nodeAssignments,src) && rgt.eval(nodeAssignments,src);
		if( oper == Oper.DISJUNCTION )
			return lft.eval(nodeAssignments,src) || rgt.eval(nodeAssignments,src);
		if( oper == Oper.NEGATION )
			return !lft.eval(nodeAssignments,src);  // ?????????
		throw new AssertionError("Unexpected case");
	}
	public String toString() {
		return toString(0);
	}
	public String toString( int depth ) {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < depth ;i++ )
            sb.append("  ");  //$NON-NLS-1$
        sb.append(oper.toString().substring(0,4)+'\n');
        sb.append(lft.toString(depth+1));
        if( rgt != null )
        	sb.append(rgt.toString(depth+1));
        return sb.toString();
	}
	@Override
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		if( rgt == null ) {
			lft.variables(ret, oper == Oper.CONJUNCTION);
			return;
		}
		lft.variables(ret, oper == Oper.CONJUNCTION);
		rgt.variables(ret, oper == Oper.CONJUNCTION);
	}
    @Override
    public MaterializedPredicate eval( Parsed target ) {
        if( oper == Oper.DISJUNCTION ) {
            MaterializedPredicate l = lft.eval(target);
            if( l == null )
                return null;
            MaterializedPredicate r = rgt.eval(target);
            if( r == null )
                return null;
            return MaterializedPredicate.union(l,r);
        }
        if( oper == Oper.CONJUNCTION) {
            MaterializedPredicate l = lft.eval(target);
            if( l == null )
                return null;
            MaterializedPredicate r = rgt.eval(target);
            if( r == null )
                return null;
            return MaterializedPredicate.join(l,r);
        }
        if( oper == Oper.DIFFERENCE) {
            MaterializedPredicate l = lft.eval(target);
            if( l == null )
                return null;
            MaterializedPredicate r = rgt.eval(target);
            if( r == null )
                return null;
            return MaterializedPredicate.difference(l,r);
        }
        return null;
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        if( oper != Oper.CONJUNCTION && oper != Oper.DISJUNCTION )
            return null;
        Predicate l = lft.isRelated(var1, var2, varDefs);
        Predicate r = rgt.isRelated(var1, var2, varDefs);
        if( l != null && r != null )
            return new CompositeExpr(l, r, oper);        
        if( l == null && r != null && oper == Oper.CONJUNCTION)
            return r;        
        if( l != null && r == null && oper == Oper.CONJUNCTION)
            return l;        
        //if( l == null && r == null )
            return null;        
    }
    
    @Override
    public Map<String, Boolean> dependencies() {
        if( oper == Oper.CONJUNCTION ) {
            Map<String, Boolean> ret = lft.dependencies();
            ret.putAll(rgt.dependencies());
            return ret;
        }
        if( oper == Oper.DIFFERENCE ) {
            Map<String, Boolean> ret = lft.dependencies();
            for( String s : rgt.dependencies().keySet() )
                ret.put(s,false);
            return ret;
        }
        if( oper == Oper.DISJUNCTION ) {
            if( rgt instanceof MaterializedPredicate ) {
                if( ((MaterializedPredicate)rgt).tuples.size() == 0 )
                    return lft.dependencies();
            }
            if( lft instanceof MaterializedPredicate ) {
                if( ((MaterializedPredicate)lft).tuples.size() == 0 )
                    return rgt.dependencies();
            }
            Map<String, Boolean> ret = new HashMap<String, Boolean>();
            for( String s : lft.dependencies().keySet() )
                ret.put(s,false);
            for( String s : rgt.dependencies().keySet() )
                ret.put(s,false);
            return ret;
        }
        if( oper == Oper.NEGATION ) {
            Map<String, Boolean> ret = new HashMap<String, Boolean>();
            for( String s : lft.dependencies().keySet() )
                ret.put(s,false);
            return ret;
        }
        throw new AssertionError("Unexpected case");
   }

}

/**
 *  a useful leaf to hang off the tree branch
 */
class True implements Predicate {
    @Override
	public boolean eval( Map<String, ParseNode> nodeAssignments,List<LexerToken> src ) {
		return true;
	}	
    @Override
	public String toString() {
		return "true";
	}
    @Override
	public String toString(int depth) {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < depth ;i++ )
            sb.append("  ");  //$NON-NLS-1$
		sb.append(toString());
		sb.append('\n');
        return sb.toString();
	}
    @Override
	public void variables( Set<String> ret, boolean optimizeEqs ) {
	}
    @Override
    public MaterializedPredicate eval(Parsed target) {
        throw new AssertionError("N/A");
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        return new HashMap<String, Boolean>();
   }
}