package qbql.lang.parse;

import java.util.Map;

import qbql.parser.ParseNode;

abstract class Attribute {
	String name;
    
	static ParseNode getPredecessor( ParseNode self, ParseNode parent ) {
		ParseNode prior = null;
		for( ParseNode child : parent.children() ) {
			if( child == self ) {
				return prior;
			}
			prior = child;
		}
		return null;
	}
	static ParseNode getSuccessor( ParseNode self, ParseNode parent ) {
		ParseNode prior = null;
		for( ParseNode child : parent.children() ) {
			if( prior == self ) {
				return child;
			}
			prior = child;
		}
		return null;
	}
	
	protected static String referredTo( String attr ) {
	    int pos = attr.indexOf('=');
        if( 0 < pos )
            return attr.substring(pos+1);
        else if( attr.endsWith("^") )
	        return attr.substring(0,attr.length()-1);
	    else if( attr.endsWith("+1") || attr.endsWith("-1") )
	        return attr.substring(0,attr.length()-2);
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
	
	abstract ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ); 
	abstract Attribute referredTo( Map<String,Attribute> varDefs ); 
}


class EqualExpr extends Attribute {
    String def;
	public EqualExpr( String name, String def ) {
	    this.name = name;
        this.def = def;
	}
	@Override
	ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ) {
		return nodeAssignments.get(def);
	}
    @Override
    Attribute referredTo(Map<String, Attribute> varDefs) {
        return varDefs.get(def);
    }
}

/**
 * Generalization of EqualExpr which assigns one value only
 * Here we assign two or more dependent values
 */
/*class SmallDomain extends Attribute {
	ArrayList<String> altDefs;
	int index = -1;
	public SmallDomain( String name, ArrayList<String> altDefs ) {
		this.name = name;
		this.altDefs = altDefs;
	}
	@Override
	public void assign( Map<String,ParseNode> nodeAssignments, final ParseNode root, final Map<String,Attribute> vars, int ind ) {
		if( nodeAssignments.containsKey(name) )
			return;
		if( index != ind && ind != -1 ) {
			index = ind;
			return;
		}
		ParseNode refNode = nodeAssignments.get(altDefs.get(index));
		if( refNode == null ) {
			//throw new AssertionError("Referenced node is not assigned");
			Attribute refVar= vars.get(altDefs.get(index));
			refVar.assign(nodeAssignments, root, vars, -1);
			refNode = nodeAssignments.get(altDefs.get(index));
		}
		nodeAssignments.put(name, refNode);
	}
	@Override
	int getLimits() {
		return altDefs.size();
	}	
}*/

	
class Parent extends Attribute {	
    String ref;
	public Parent( String name ) {
		this.name = name;
		ref = referredTo(name);
	}
	@Override
	ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ) {
        ParseNode refNode = null;
        if( ref != null )
            refNode = nodeAssignments.get(ref);
		if( refNode == null )
			return null;
		ParseNode parent = refNode.parent();//root.parent(refNode.from, refNode.to);
		return parent;
	}
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(ref);
    }
}

class Predecessor extends Attribute {	
    String ref;
	public Predecessor( String name ) {
		this.name = name;
        ref = referredTo(name);
	}
	@Override
	ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ) {
        ParseNode refNode = null;
        if( ref != null )
            refNode = nodeAssignments.get(ref);
		if( refNode == null )
			return null;
		ParseNode parent = refNode.parent(); //root.parent(refNode.from, refNode.to);
		if( parent == null )
			return null;
		return getPredecessor(refNode, parent);
	}
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(ref);
    }
}

class Successor extends Attribute {	
    String ref;
	public Successor( String name ) {
		this.name = name;
        ref = referredTo(name);
	}
	@Override
	ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ) {
        ParseNode refNode = null;
        if( ref != null )
            refNode = nodeAssignments.get(ref);
		if( refNode == null )
			return null;
		ParseNode parent = refNode.parent(); //root.parent(refNode.from, refNode.to);
		if( parent == null )
			return null;
		return getSuccessor(refNode, parent);
	}
    @Override
    Attribute referredTo( Map<String, Attribute> varDefs ) {
        return varDefs.get(ref);
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
    ParseNode navigate( Map<String,ParseNode> nodeAssignments, ParseNode root ) {
        return nodeAssignments.get(name); 
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

    /*@Override
    public String toString() {
        return rel+'.'+name;
    }*/
}





	

