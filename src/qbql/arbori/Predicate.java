package qbql.arbori;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.Parsed;

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
	 * @param varDefs 
	 * @return
	 */
    boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String, Attribute> varDefs );
    
    /**
     * Gather variables in the predicate expression
     * @param ret -- collect stuff here
     * @param optimizeEqs TODO
     * @param optimizeEqs -- if encounted disjunction, then can't assume that any variable find around equality predicate is dependent anymore
     */
	public void variables( Set<String> ret, boolean optimizeEqs );
	
    /**
     * Output variables in the predicate expression
     * Subset of variables (for rules with disjunction)
     * @param ret -- collect stuff here
     * @param optimizeEqs TODO
     * @param optimizeEqs -- if encounted disjunction, then can't assume that any variable find around equality predicate is dependent anymore
     */
    public void signature( Set<String> ret );
    
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
    
    /**
     * Get copy in another program
     * @param prg
     * @return
     */
    public Predicate copy( Program prg );

    void adjustIsFunFlag(Set<String> independentAttributes);
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
    public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
        Predicate ref = program.namedPredicates.get(name);
        if( ref == null )
            throw new AssertionError("Unreferenced predicate variable "+name);
        if( !(ref instanceof MaterializedPredicate) )
            throw new AssertionError("Unevaluated predicate variable "+name);
        return ref.eval(attributePositions,tuple, src, varDefs);
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
    public void signature( Set<String> ret ) {
        variables(ret, false);
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

    @Override
    public Predicate copy( Program prg ) {
        return new PredRef(name, prg);
    }
    
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
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
	ParseNode getNode( String nodeVar, Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String, Attribute> varDefs ) throws AssertionError {
		final Integer attrPos = attributePositions.get(nodeVar);
		ParseNode ret = null;
		if( attrPos != null )
			ret =  tuple[attrPos];  
		else {  // unit.test #11 
			Attribute attr = varDefs.get(nodeVar);
			return attr.lookup(attributePositions, tuple, varDefs);
		}
		return ret;
	}
	static void variables( String var, Set<String> ret ) {		
		ret.add(var);
		if( 0 < var.indexOf("<<") ) {
            String prefix = var.substring(0,var.indexOf("<<"));
            variables(prefix, ret);
            variables(var.substring(var.indexOf("<<")+2), ret);
		} else if( 0 < var.indexOf("<=") ) {
            String prefix = var.substring(0,var.indexOf("<="));
            variables(prefix, ret);
            variables(var.substring(var.indexOf("<=")+2), ret);
        } else if( 0 < var.indexOf('<') ) {
            String prefix = var.substring(0,var.indexOf('<'));
            variables(prefix, ret);
            variables(var.substring(var.indexOf('<')+1), ret);
        } else if( 0 < var.indexOf('=') ) {
            String prefix = var.substring(0,var.indexOf('='));
            variables(prefix, ret);
            variables(var.substring(var.indexOf('=')+1), ret);
        } else if( var.endsWith("^") ) {
			variables(var.substring(0,var.length()-1), ret);
        } else {//if( var.endsWith("-1") || var.endsWith("+1") )
			  //variables(var.substring(0,var.length()-2), ret);
			int pos = var.lastIndexOf('-');
			int pos2 = var.lastIndexOf('+');
			if( pos < pos2 ) 
				pos = pos2;
			if( 0 < pos )
				try {
					String suffix = var.substring(pos);
					final int n = Integer.parseInt(suffix);
					variables(var.substring(0,pos), ret);
				} catch( NumberFormatException e ) {}
		} 			
	}
    @Override
    public void signature( Set<String> ret ) {
        variables(ret, false);
    }
    @Override
    public MaterializedPredicate eval( Parsed target ) {
        return null;
    }
    
    @Override
    public Predicate copy( Program prg ) {
        return this;
    }

    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
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
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		ParseNode node = getNode(nodeVar, attributePositions,tuple, varDefs);
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
    public void signature( Set<String> ret ) {
        variables(ret, false);
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
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
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
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		ParseNode node1 = getNode(nodeVar1, attributePositions,tuple, varDefs);
		if( node1 == null )
			return false;
		ParseNode node2 = getNode(nodeVar2, attributePositions,tuple, varDefs);
		if( node2 == null )
			return false;
		String content1 = node1.content(src,true);
		String content2 = node2.content(src,true);
		return content1.equalsIgnoreCase(content2);
	}
	public String toString() {
		return "?"+nodeVar1+" = ?"+nodeVar2;
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar1,ret);
		variables(nodeVar2,ret);
	}
    @Override
    public void signature( Set<String> ret ) {
        variables(ret, false);
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
	Boolean addWsDividers;
	public NodeMatchingSrc( String nodeVar, String literal, Boolean addWsDividers ) {
		this.nodeVar = nodeVar;
		if( literal.charAt(0)!='\'' )
			throw new AssertionError("expected string literal");
		this.literal = literal.substring(1,literal.length()-1);
		if( literal.charAt(1)=='\'' )
			this.literal = this.literal.substring(1,this.literal.length()-1);
		this.addWsDividers = addWsDividers;
	}
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		ParseNode node = getNode(nodeVar, attributePositions,tuple, varDefs);
		if( node == null )
			return false;
		return node.content(src).equalsIgnoreCase(literal);
	}
	public String toString() {
		return "?"+nodeVar+" = '"+literal+"'";
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar,ret);
	}
    @Override
    public void signature( Set<String> ret ) {
        variables(ret, false);
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
 */
class AncestorDescendantNodes extends IdentedPredicate {
	String a;
	String d;
	enum Type { CLOSEST, TRANSITIVE;
	 	public String oper() {
			if( this == CLOSEST )
	            return "<";
	        else if( this == TRANSITIVE )
	            return "<<";
			throw new AssertionError("Unexpected AncestorDescendant type: "+this.toString());
		}
    };
	Type type;
	public AncestorDescendantNodes(  String ancestor, String descendant, Type type ) {
		this.a = ancestor;
		this.d = descendant;
		this.type = type;
	}
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		ParseNode nodeA = getNode(a, attributePositions,tuple, varDefs);
		if( nodeA == null )
			return false;
		ParseNode nodeD = getNode(d, attributePositions,tuple, varDefs);
		if( nodeD == null )
			return false;
		if( type == Type.CLOSEST )
		    return nodeA.from <= nodeD.from && nodeD.to <= nodeA.to 
		    && (nodeA.from != nodeD.from || nodeD.to != nodeA.to);
		else if( type == Type.TRANSITIVE )
		    return nodeA.from <= nodeD.from && nodeD.to <= nodeA.to;
		//else
			throw new AssertionError("Unexpected AncestorDescendant type: "+type.toString());
	}
    @Override
	public String toString() {
        return a+" "+type.oper()+" "+d;
	}
    @Override
	public void variables( Set<String> ret, boolean optimizeEqs ) {
        if( optimizeEqs )
            variables(a+type.oper()+d,ret);
        else {
            //isFunctional = false;
            variables(a,ret);
            variables(d,ret);           
        }

	}
    @Override
    public void signature( Set<String> ret ) {
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
    
    Set<Tuple> innerLoop( ParseNode node, MaterializedPredicate anc ) {
    	if( 1 < anc.arity() )
    		throw new AssertionError("1 < anc.arity()");
    	Set<Tuple> ret = new TreeSet<Tuple>();
        if( node != null && type == Type.CLOSEST ) 
            node = node.parent();
        while( node != null ) {
        	Tuple candidate  = new Tuple(new ParseNode[]{node});
        	if( anc.getTuples().contains(candidate) )
        		 ret.add(candidate);
            node = node.parent();
        }             
        return ret;
    }

}

/**
 * Comparing head or tail of one node with the other (or bind var, for that matter).
 * Or arithmetic expression
 */
abstract class Position {
    protected PositionalRelation master;
    void setMaster( PositionalRelation master ) {
        this.master = master;
    }
    protected String name = null;
    Position( String name ) {
    	if( name == null ) // post initialized
    		return;
        setName(name);
    }
	public void setName( String name ) {
		this.name = name.trim();
        int len = this.name.length();
        if( this.name.charAt(0) == '(' && this.name.charAt(len-1) == ')' )
        	this.name = this.name.substring(1, len-1);
	}

    abstract int eval( Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ); 
    // one generic method instead of code duplication in subclasses
    public Position clone() {
        try {
            Constructor<? extends Position> ctor = getClass().getDeclaredConstructor(String.class);
            Position ret = (Position) ctor.newInstance(name);
            ret.master = master; 
            return ret;
        } catch( Exception e ) {
            throw new AssertionError("Position clone() reflection failed: "+e.getMessage());
        }
    }
}
class Head extends Position {
    Head( String name ) {
        super(name);
    }
    @Override
    public int eval( Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ) {
        ParseNode nodeA = master.getNode(name, attributePositions,tuple, varDefs);
        return nodeA.from;
    }
    @Override
    public String toString() {
        return "["+name;
    }    
}
class Tail extends Position {
    Tail( String name ) {
        super(name);
    }
    @Override
    public int eval( Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ) {
        ParseNode nodeA = master.getNode(name, attributePositions,tuple, varDefs);
        return nodeA.to;
    }    
    @Override
    public String toString() {
        return name+")";
    }    
}
class BindVar extends Position {
    BindVar( String name ) {
        super(name);
    }
    @Override
    public int eval( Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ) {
    	GlobalMap globals = master.prog.getGlobals();
    	if( globals != null ) {
    		Object object = globals.get(name);
    		if( object instanceof String ) {
    			try {
    			   return Integer.parseInt((String) object);
    			} catch( NumberFormatException e ) {}
    		}
    		if( object instanceof Integer ) {
    			try {
    			   return (Integer) object;
    			} catch( NumberFormatException e ) {}
    		}
    	}
    	// evaluate bind variables via reflection over program or struct instances
        try {
        	
            Field f = master.prog.getClass().getField(name);
            f.setAccessible(true);
            return f.getInt(master.prog);
        } catch( NoSuchFieldException e ) {
            try {
				Field f = master.prog.struct.getClass().getField(name);
	            f.setAccessible(true);
	            return f.getInt(master.prog.struct);
			} catch (Exception e1) {
	            throw new AssertionError("Bind var '"+name+"' not found: "+e1.getMessage());
			}        	
        } catch( Exception e ) {
            throw new AssertionError("Bind var '"+name+"' not found: "+e.getMessage());
        }
    }    
    @Override
    public String toString() {
        return ":"+name;
    }    
}
class Composite extends Position {
    Position child;  
    int addendum; 
    Composite( Position child, int addendum ) {
        super(child.name); // same name to get dependencies and variables
        this.child = child;
        this.addendum = addendum;
    }
    @Override
    public int eval( Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ) {
        return child.eval(attributePositions,tuple, varDefs)+addendum;
    }    
    @Override
    public String toString() {
        return addendum+"+"+child.toString();
    } 
    @Override
    void setMaster( PositionalRelation master ) {
        super.setMaster(master);
        child.setMaster(master);
    }
    public Position clone() {
        return new Composite(child,addendum);
    }
}

class PositionalRelation extends IdentedPredicate {
        
    Position a;
    Position b;
	Program prog;
	boolean isReflexive;
    boolean isGT;
	public PositionalRelation( Position a, Position b, boolean isReflexive, boolean isGT, Program prog ) {
        this.prog = prog;
		this.a = a;
		this.a.setMaster(this);
		this.b = b;
        this.b.setMaster(this);
        this.isReflexive = isReflexive;
        this.isGT = isGT;
	}
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {	    
	    if( isReflexive && isGT ) 
	        return a.eval(attributePositions,tuple, varDefs) <= b.eval(attributePositions,tuple, varDefs);
	    else if( isReflexive && !isGT ) 
	        return a.eval(attributePositions,tuple, varDefs) == b.eval(attributePositions,tuple, varDefs);
        else if( !isReflexive && isGT ) 
            return a.eval(attributePositions,tuple, varDefs) < b.eval(attributePositions,tuple, varDefs);
        else
            throw new AssertionError("!isReflexive && !isGT");
	}
	
	public String toString() {
		return a.toString()+" <"+(isReflexive?"=":"")+" "+b.toString();
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		if( ! (a instanceof BindVar) )
			variables(a.name,ret);
		if( ! (b instanceof BindVar) )
			variables(b.name,ret);
	}
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        if( a instanceof BindVar || b instanceof BindVar )
            return null;
        Attribute attr1 = varDefs.get(a.name);
        Attribute attr2 = varDefs.get(b.name);
        if( attr1.isDependent(var1,varDefs)&&attr2.isDependent(var2,varDefs) 
         || attr2.isDependent(var1,varDefs)&&attr1.isDependent(var2,varDefs) )
            return this;
        else
            return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        int pos = a.name.indexOf('.');
        if( 0 < pos  )
            ret.put(a.name.substring(0,pos),true);
        pos = b.name.indexOf('.');
        if( 0 < pos  )
            ret.put(b.name.substring(0,pos),true);
        return ret;
    }
    
    @Override
    public Predicate copy( Program prg ) {
        return new PositionalRelation(a.clone(),b.clone(),isReflexive,isGT,prg);
    }
    

}


class ChildNumRelation extends IdentedPredicate {
    
    int size;
    String nodeVar;
	Program prog;
	boolean isReflexive;
    boolean isGT;
	public ChildNumRelation( int size, String node, boolean isReflexive, boolean isGT, Program prog ) {
        this.size = size;
		this.nodeVar = node;
        this.isReflexive = isReflexive;
        this.isGT = isGT;
	}
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		ParseNode n = getNode(nodeVar, attributePositions,tuple, varDefs);
	    if( isReflexive && isGT ) 
	        return size <= n.children().size();
	    else if( isReflexive && !isGT ) 
	        return size == n.children().size();
        else if( !isReflexive && isGT ) 
            return size < n.children().size();
        else
            throw new AssertionError("!isReflexive && !isGT");
	}
	
	public String toString() {
		return size+" <"+(isReflexive?"=":"")+" #"+nodeVar;
	}
	public void variables( Set<String> ret, boolean optimizeEqs ) {
		variables(nodeVar,ret);
	}
    @Override
    public void signature( Set<String> ret ) {
        variables(ret, false);
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
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
	    if( isFunctional )
	        return true;
		ParseNode nodeA = getNode(a, attributePositions,tuple, varDefs);
		if( nodeA == null )
			return false;
		ParseNode nodeB = getNode(b, attributePositions,tuple, varDefs);
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
    public void signature(Set<String> ret) {
        variables(a,ret);
        variables(b,ret);           
    }
    
    @Override
    ParseNode getNode( String nodeVar, Map<String, Integer> attributePositions, ParseNode[] tuple, Map<String,Attribute> varDefs ) throws AssertionError {
        try {
            return super.getNode(nodeVar, attributePositions,tuple, varDefs);
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

    @Override
	public void adjustIsFunFlag( Set<String> independentVars ) {		
        if( !isFunctional )
            return;
        if( !independentVars.contains(a) && !independentVars.contains(b) )
            isFunctional = false;
	}
}

class JSFunc implements Predicate {
    String name;
    Program program; 
    List<String> args;
    
    public JSFunc( String name, Program program, List<String> args ) {
        this.name = name;
        this.program = program;
        this.args =args;
    }

    @Override
    public boolean eval( Map<String, Integer> attributePositions, ParseNode[] t, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		try {
	    	Invocable invocable = (Invocable) program.getEngine();
	    	if( invocable == null )
	    		return true;
	    	
		    Map<String,ParseNode> tuple = new HashMap<String,ParseNode>();
		    for ( String colName : attributePositions.keySet() ) {
		        ParseNode node = t[attributePositions.get(colName)]; 
		        tuple.put(colName, node);
		    }
	    	GlobalMap globals = program.getGlobals();
	    	if( globals == null )
	    		return false;
			globals.put("tuple", tuple);
	    	
	    	if( args.size() == 0 )
	    		return (Boolean) invocable.invokeFunction(name);
	    	if( args.size() == 1 )
	    		return (Boolean) invocable.invokeFunction(name,args.get(0));
	    	if( args.size() == 2 )
	    		return (Boolean) invocable.invokeFunction(name,args.get(0),args.get(1));
	    	throw new AssertionError("Wrong number of arguments (="+args.size()+")");
		} catch( Exception e ) {
			e.printStackTrace();
		}
    	        
        return false;
    }

    @Override
    public String toString( int depth ) {
        return "."+name;
    }

    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
    }
    @Override
    public void signature( Set<String> ret ) {
    }

    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }

    @Override
    public Map<String, Boolean> dependencies() {
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        return ret;
    }

    @Override
    public Predicate copy( Program prg ) {
        return new JSFunc(name, prg, args);
    }
    
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}

	@Override
	public MaterializedPredicate eval(Parsed target) {
		try {
	    	Invocable invocable = (Invocable) program.getEngine();
	    	if( invocable == null )
	    		return new False().eval(target);
	    	
		    /* Not on tuple level
		     * Map<String,ParseNode> tuple = new HashMap<String,ParseNode>();
		    for ( String colName : attributePositions.keySet() ) {
		        ParseNode node = t[attributePositions.get(colName)]; 
		        tuple.put(colName, node);
		    }
	    	program.globals.put("tuple", tuple);*/
	    	
			if( (Boolean) invocable.invokeFunction(name) )
				new True().eval(target);
			else
				new False().eval(target);
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return new False().eval(target);
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
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		if( oper == Oper.CONJUNCTION )
			return lft.eval(attributePositions,tuple,src, varDefs) && rgt.eval(attributePositions,tuple,src, varDefs);
		if( oper == Oper.DISJUNCTION )
			return lft.eval(attributePositions,tuple,src, varDefs) || rgt.eval(attributePositions,tuple,src, varDefs);
		if( oper == Oper.NEGATION )
			return !lft.eval(attributePositions,tuple,src, varDefs);  // ?????????
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
			lft.variables(ret, optimizeEqs && oper == Oper.CONJUNCTION);
			return;
		}
		lft.variables(ret, optimizeEqs && oper == Oper.CONJUNCTION);
		rgt.variables(ret, optimizeEqs && oper == Oper.CONJUNCTION);
	}
    @Override
    public void signature( Set<String> ret ) {
        if( rgt == null ) {
            lft.signature(ret);
            return;
        }
        if( oper == Oper.CONJUNCTION ) {
            lft.signature(ret);
            rgt.signature(ret);
        } else if( oper == Oper.DISJUNCTION ) {
            Set<String> first = new HashSet<String>();
            lft.signature(first);
            Set<String> second = new HashSet<String>();
            rgt.signature(second);   
            for( String f : first )
            	for( String s : second ) {
        			for( int i = f.indexOf('.'); 0 < i ; i = f.indexOf('.',i+1) ) 
        				f = f.substring(i+1);
        			for( int i = s.indexOf('.'); 0 < i ; i = s.indexOf('.',i+1) ) 
        				s = s.substring(i+1);
        			if( f.equals(s) )
        				ret.add(f);
            	}
            //first.retainAll(second);
            //ret.addAll(first);
        } else if( oper == Oper.DIFFERENCE ) {
            lft.signature(ret);        
        }
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
        if( oper == Oper.NEGATION) {
            MaterializedPredicate l = lft.eval(target);
            if( l == null )
                return null;
            if( 0 < l.attributes.size() )
                return null;
            if( l.tuples.size() == 0 ) {
            	MaterializedPredicate ret = new MaterializedPredicate(new ArrayList<String>(),target.getSrc(),"DEE");
            	ret.addContent(new ParseNode[0]);
                return ret;
            } else 
                return new MaterializedPredicate(new ArrayList<String>(),target.getSrc(),"DUM");
        }
       return null;
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        if( oper != Oper.CONJUNCTION && oper != Oper.DISJUNCTION && oper != Oper.NEGATION )
            return null;
        Predicate l = lft.isRelated(var1, var2, varDefs);
        Predicate r = null;
        if( oper != Oper.NEGATION )
            r = rgt.isRelated(var1, var2, varDefs);
        if( l != null && r != null )
            return new CompositeExpr(l, r, oper);        
        if( l == null && r != null && oper == Oper.CONJUNCTION)
            return r;        
        if( l != null && r == null && oper == Oper.CONJUNCTION)
            return l;        
        if( l != null && r == null && oper == Oper.NEGATION)
            return this;        
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
                if( ((MaterializedPredicate)rgt).cardinality() == 0 )
                    return lft.dependencies();
            }
            if( lft instanceof MaterializedPredicate ) {
                if( ((MaterializedPredicate)lft).cardinality() == 0 )
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

    @Override
    public Predicate copy( Program prg ) {
        CompositeExpr ret = new CompositeExpr(lft, rgt, oper);
        ret.lft = lft.copy(prg);
        if( rgt != null  )
            ret.rgt = rgt.copy(prg);
        return ret;
    }
    
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
        lft.adjustIsFunFlag(independentAttributes);
        if( rgt != null  )
            rgt.adjustIsFunFlag(independentAttributes);
	}
}

/**
 *  A useful leaf to hang off the tree branch
 *  Also used to evaluate boolean bind variables
 */
class True implements Predicate {
    @Override
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
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
    public void signature( Set<String> ret ) {
        variables(ret, false);
    }
    @Override
    public MaterializedPredicate eval(Parsed target) {
    	MaterializedPredicate ret = new MaterializedPredicate(new ArrayList<String>(),target.getSrc(),"DEE");
    	ret.addContent(new ParseNode[0]);
        return ret;
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        return new HashMap<String, Boolean>();
    }
    @Override
    public Predicate copy( Program prg ) {
        return this;
    }
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}
}
class False implements Predicate {
    @Override
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		return false;
	}	
    @Override
	public String toString() {
		return "false";
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
    public void signature( Set<String> ret ) {
        variables(ret, false);
    }
    @Override
    public MaterializedPredicate eval(Parsed target) {
        	MaterializedPredicate ret = new MaterializedPredicate(new ArrayList<String>(),target.getSrc(),"DUM");
            return ret;
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        return new HashMap<String, Boolean>();
    }
    @Override
    public Predicate copy( Program prg ) {
        return this;
    }   
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}
}

class Header implements Predicate {  // False can be subclassed from Header (with empty set of attributes)
	ArrayList<String> attributes;    	
	Header( ArrayList<String> attributes ) {
		this.attributes = attributes;
	}
	@Override
	public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
		return false;
	}	
    @Override
	public String toString() {
		return "["+attributes.toString()+"...]";
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
        ret.addAll(attributes);
	}
    @Override
    public void signature( Set<String> ret ) {
        variables(ret, false);
    }
    @Override
    public MaterializedPredicate eval(Parsed target) {
        MaterializedPredicate ret = new MaterializedPredicate(attributes,target.getSrc(),toString());
        return ret;
    }
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return null;
    }
    @Override
    public Map<String, Boolean> dependencies() {
        return new HashMap<String, Boolean>();
    }
    @Override
    public Predicate copy( Program prg ) {
        return this;
    }
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}
}
