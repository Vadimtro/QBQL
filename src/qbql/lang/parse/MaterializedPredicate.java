package qbql.lang.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.util.Util;

/**
 * Arbori program consists of [symbolic] predicates, which are evaluated (materialized) into relations
 * @author Dim
 */
public class MaterializedPredicate 
    extends IndependentAttribute // N-ary relation can be viewed as unary relation with one composite attribute
    implements Predicate  {
    
	private Map<String,Integer> attributePositions = new HashMap<String,Integer>();
	ArrayList<String> attributes;
	ArrayList<ParseNode[]> tuples = new ArrayList<ParseNode[]>();
	List<LexerToken> src;
	
	public MaterializedPredicate( ArrayList<String> attributes, List<LexerToken> src, String name ) {
	    super(name,null);
		this.attributes = attributes;
		for( int i = 0; i < attributes.size(); i++ ) 
			attributePositions.put(attributes.get(i), i);
		this.src = src;
		
		//this.name = name;
	}
	public MaterializedPredicate( String name, MaterializedPredicate src ) {
        super(name,null);
		this.attributes = src.attributes;
		this.attributePositions = src.attributePositions;
		this.tuples = src.tuples; //(ArrayList<ParseNode[]>) src.tuples.clone();
		this.src = src.src;
	}
	
	@Override
    public MaterializedPredicate getContent(){
        return this;
    }
    
	/**
	 * Tuple evaluation: not well defined
	 */
    @Override
    public boolean eval( Map<String, ParseNode> nodeAssignments, List<LexerToken> src ) {
        throw new AssertionError("N/A");
    }
    /**
     * Variables for [symbolic] predicate evaluation are not needed if predicate is materialized already
     */
    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
        ret.addAll(attributes);
    }
    /**
     * This method is for symbolic predicates only
     */
    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        throw new AssertionError("N/A");
    }
	
	
	public void add( Map<String, ParseNode> vector ) {
		ParseNode[] tuple = new ParseNode[attributes.size()];
		for( int i = 0; i < tuple.length; i++ ) {
			tuple[i] = vector.get(attributes.get(i));
		}
		addContent(tuple);
	}
	private void add( ParseNode[] tuple ) {
		addContent(tuple);
	}
	
	void addContent( ParseNode[] tuple ) {
		for( ParseNode[] t : tuples ) {
			boolean match = true;
			for( int i = 0; i < t.length; i++ ) 
				if( t[i].from != tuple[i].from || t[i].to != tuple[i].to ) {
					match = false;
					break;
				}
			if( match )
				return;		
		}
		tuples.add(tuple);
	}
	
	/**
	 * Get value of a cell in the content table
	 * @param tupleNum
	 * @param attribute
	 * @return
	 */
	public ParseNode getAttribute( int tupleNum, String attribute ) {
		ParseNode[] tuple = tuples.get(tupleNum);
		return tuple[attributePositions.get(attribute)];
	}
	
	public String getAttribute( int colPos ) {
	    return attributes.get(colPos);
	}
    public Integer getAttribute( String colName ) {
        return attributePositions.get(colName);
    }

    public int arity() {
        return attributes.size();
    }
	
	public int cardinality() {
		return tuples.size();
	}
	
	/*public void assign( Map<String,ParseNode> nodeAssignments, final ParseNode root, final Map<String,Attribute> vars, int index ) {
		for( String field : attributes ) {
			ParseNode node = getAttribute(index, field);
			nodeAssignments.put(name+'.'+field, node);            		            			
		}
	}*/
	
	/**
	 * Construct unary relation when evaluating symbolic predicate. We have content (viewed as unary relation with complex attribute) already.
	 */
	@Override
	public void initContent( ParseNode root, List<LexerToken> src, Map<String, Attribute> varDefs, String predVar ) {
	    // NOOP
	}
	@Override
	int getLimits() {
		return tuples.size();
	}
	
	
	@Override
	public String toString() {
		return toString(0);
	}
	public String toString( int ident ) {
        StringBuilder ret = new StringBuilder("");
        //ret.append(header.keySet()+"\n");
        // no commas
        ret.append("[");
        for( int i = 0; i < attributes.size(); i++ )
        	ret.append((i>0?"         ":"")+attributes.get(i));
        ret.append("]\n");
        for( int j = 0; j < tuples.size(); j++ ) { 
        	boolean firstTuple = true;
            for( int i = 0; i < attributes.size(); i++ ) {
        		String value = "N/A (null)";
        		ParseNode node = tuples.get(j)[i];
        		if( node != null )
        		    value = "["+tuples.get(j)[i].from+","+tuples.get(j)[i].to+")";
        		ret.append((firstTuple?Util.identln(ident," "):"  "));
        		ret.append(value);
        		ret.append(" ");
                if( node != null )
                    ret.append(mnemonics(node.from, node.to));
        		firstTuple = false;			
            }
        	ret.append("\n");
        }
        return ret.toString();
	}
	private String mnemonics( int from, int to ) {
		if( from + 1 == to)
			return Util.padln(8<src.get(from).content.length() ? src.get(from).content.substring(0,8) : src.get(from).content, 8);
		else {
			StringBuilder ret = new StringBuilder("\"");
			for( int i = from; i < to && i < from+8 ; i++ ) {
				ret.append(src.get(i).content.charAt(0));
			}
			ret.append('\"');
			return ret.toString();
		}
	}

    /*@Override
    public void eqNodes( Map<String, Attribute> varDefs ) {
        throw new AssertionError("N/A");
    }*/
	
    @Override
    public MaterializedPredicate eval(Parsed target) {
        return this;//new MaterializedPredicate(this);
    }
    
    public static MaterializedPredicate union( MaterializedPredicate x, MaterializedPredicate y ) {
        ArrayList<String> header = new ArrayList<String>();
        header.addAll(x.attributes);
        header.retainAll(y.attributes);        
        MaterializedPredicate ret = new MaterializedPredicate(header,x.src,null);
        for( ParseNode[] tupleX: x.tuples ){
            ParseNode[] retTuple = new ParseNode[header.size()];
            for( String attr : ret.attributes ) {
                retTuple[ret.attributePositions.get(attr)] = tupleX[x.attributePositions.get(attr)];
            }
            ret.addContent(retTuple);
        }
        for( ParseNode[] tupleY: y.tuples ){
            ParseNode[] retTuple = new ParseNode[header.size()];
            for( String attr : ret.attributes ) {
                retTuple[ret.attributePositions.get(attr)] = tupleY[y.attributePositions.get(attr)];
            }
            ret.addContent(retTuple);
        }
        return ret;
    }
    public static MaterializedPredicate join( MaterializedPredicate x, MaterializedPredicate y ) {
        ArrayList<String> header = new ArrayList<String>();
        header.addAll(x.attributes);
        for( String s : y.attributes )
            if( !x.attributes.contains(s) )
                header.add(s);       
        MaterializedPredicate ret = new MaterializedPredicate(header,x.src,null);
        for( ParseNode[] tupleX: x.tuples )
            for( ParseNode[] tupleY: y.tuples ) {                
                ParseNode[] retTuple = new ParseNode[header.size()];
                for( String attr : ret.attributes ) {
                    Integer iX = x.attributePositions.get(attr);
                    Integer xAttr = iX;
                    Integer iY = y.attributePositions.get(attr);
                    Integer yAttr = iY;
                    Integer iRet = ret.attributePositions.get(attr);
                    if( xAttr == null )
                        retTuple[iRet] = tupleY[iY];
                    else if( yAttr == null )
                        retTuple[iRet] = tupleX[iX];
                    else {
                        if( tupleY[iY].from != tupleX[iX].from ||
                            tupleY[iY].to != tupleX[iX].to 
                        ) {
                            retTuple = null;
                            break;
                        } else
                            retTuple[iRet] = tupleX[iX];
                    }
                }
                if( retTuple != null )
                    ret.addContent(retTuple);
            }
        return ret;
    }
    public static MaterializedPredicate difference( MaterializedPredicate x, MaterializedPredicate y ) {
        ArrayList<String> header = new ArrayList<String>();
        header.addAll(x.attributes);
        /*for( String s : y.attributes )
            if( !x.attributes.contains(s) )
                throw new AssertionError("! y.attributes <= x.attributes");*/       
        MaterializedPredicate ret = new MaterializedPredicate(header,x.src,null);
        for( ParseNode[] tupleX: x.tuples ) {
            boolean foundMatch = false;
            for( ParseNode[] tupleY: y.tuples ) {
                boolean tuplesMatch = true;
                for( String attr : y.attributes ) {                    
                    Integer iY = y.attributePositions.get(attr);
                    Integer iX = x.attributePositions.get(attr);
                    if( iX == null ) 
                        continue;
                    
                    if( tupleY[iY].from != tupleX[iX].from ||
                            tupleY[iY].to != tupleX[iX].to ) {
                        tuplesMatch = false;
                        break;
                    }
                }
                if( tuplesMatch ) {
                    foundMatch = true;
                    break;
                }                    
            }
            if( ! foundMatch )
                ret.addContent(tupleX);
        }
        return ret;
    }
    
    /**
     * After materialized predicate evaluation trim attributes
     * Bail out if there is name conflict
     * TODO: leave prefix but wrap name in double quotes?
     */
    public void trimAttributes() {
        Map<String,Integer> trimmedAttributePositions = new HashMap<String,Integer>();
        ArrayList<String> trimmedAttributes = new ArrayList<String>();
        for( int i = 0; i < attributes.size(); i++ ) {
            String attribute = attributes.get(i);
            /*int pos = attribute.indexOf('=');
            if( 0 < pos )
                attribute = attribute.substring(0,pos);
            else*/ {
                int pos = attribute.lastIndexOf('.');
                if( 0 < pos )
                    attribute = attribute.substring(pos+1);
                if( trimmedAttributePositions.containsKey(attribute) )
                    if( 0 < pos )
                        return;
            }
            trimmedAttributePositions.put(attribute, i);
            trimmedAttributes.add(attribute);
        }
        
        attributePositions = trimmedAttributePositions;
        attributes = trimmedAttributes;
    }

    public static MaterializedPredicate filteredCartesianProduct( MaterializedPredicate x, MaterializedPredicate y, Predicate filter, Map<String,Attribute> varDefs, ParseNode root ) {
        ArrayList<String> header = new ArrayList<String>();
        
        //header.addAll(x.attributes);
        //header.addAll(y.attributes);        
        // add dependent attributes
        for( String z : varDefs.keySet() ) {
            /*if( x.name != null ) { 
                if( !z.contains(x.name+'.') )   // fails on proc = "procedures".par_list-1 (default_par.prg
                    continue;
            }*/ 
            Attribute attr = varDefs.get(z);
            for( String s : x.attributes ) {
                if( x.name != null && s.indexOf('.') < 0 )
                    s = x.name+'.'+s;
                if( attr.isDependent(s, varDefs)) {
                    header.add(z);
                    break;
                }
            }
        }
        for( String z : varDefs.keySet() ) {
            if( header.contains(z) )
                continue;
            
            /*if( y.name != null ) {
                if( !z.contains(y.name+'.') )
                    continue;
            }*/ 
            Attribute attr = varDefs.get(z);
            for( String s : y.attributes ) {
                if( y.name != null && s.indexOf('.') < 0 )
                    s = y.name+'.'+s;
                if( attr.isDependent(s, varDefs)) {
                    header.add(z);
                    break;
                }
            }
        }
                
        MaterializedPredicate ret = new MaterializedPredicate(header,x.src,null);
        for( ParseNode[] tupleX: x.tuples )
            MID: for( ParseNode[] tupleY: y.tuples ) {
                Map<String,ParseNode> nodeAssignments = new HashMap<String,ParseNode>();
                ParseNode[] retTuple = new ParseNode[header.size()];
                for( String attr : ret.attributes ) {
                    //Integer iRet = ret.attributePositions.get(attr);  redundant; do it afterwards
                    if( x.name != null && attr.startsWith(x.name) ) {
                        Integer iX = x.attributePositions.get(attr.substring(x.name.length()+1));
                        if( iX == null ) // will assign via chain of dependencies afterwards
                            continue;
                        //retTuple[iRet] = tupleX[iX];
                        nodeAssignments.put(attr, tupleX[iX]);                        
                        continue;
                    }
                    if( y.name != null && attr.startsWith(y.name) ) {
                        Integer iY = y.attributePositions.get(attr.substring(y.name.length()+1));
                        if( iY == null ) // will assign via chain of dependencies afterwards
                            continue;
                        //retTuple[iRet] = tupleY[iY];
                        nodeAssignments.put(attr, tupleY[iY]);                        
                        continue;
                    }                    
                    Integer i = x.attributePositions.get(attr);
                    if( i != null ) {
                        //retTuple[iRet] = tupleX[i];
                        nodeAssignments.put(attr, tupleX[i]);
                        continue;
                    }
                    i = y.attributePositions.get(attr);
                    if( i != null ) {
                        //retTuple[iRet] = tupleY[i];
                        nodeAssignments.put(attr, tupleY[i]);
                        continue;
                    }
                }
                for( String attr : ret.attributes ) {
                    if( !assignDependencyChain(attr,nodeAssignments,varDefs,root) )
                        continue MID;
                    Integer iRet = ret.attributePositions.get(attr);
                    retTuple[iRet] = nodeAssignments.get(attr);
                }
                // now that all tuple attributes are assigned can evaluate if it satisfies the predicate
                if( filter.eval(nodeAssignments, x.src) )
                    ret.addContent(retTuple);
            }
        return ret;
    }
    
    /**
     * Attribute.navigate amended to recursive navigation
     * @param name - attribute name
     * @param nodeAssignments
     * @param varDefs
     * @param root
     * @return false if 
     */
    static boolean assignDependencyChain( String name, Map<String,ParseNode> nodeAssignments, Map<String,Attribute> varDefs, ParseNode root ) {
        ParseNode ret = /*attr.navigate(nodeAssignments, root); //*/nodeAssignments.get(name);
        if( ret != null )
            return true;
            
        Attribute attr = varDefs.get(name);
        if( attr instanceof MaterializedPredicate )
            return true;  
        Attribute ref = attr.referredTo(varDefs);
        if( ref == null )
            return true; // variable not joined yet
        if( !assignDependencyChain(ref.name, nodeAssignments, varDefs, root) )
            return false;;
        ParseNode value = attr.navigate(nodeAssignments, root);
        if( value == null )
            return false;
        nodeAssignments.put(attr.name, value);
        return true;
       
    }
    @Override
    public Map<String, Boolean> dependencies() {        
        throw new AssertionError("Not supposed to be called");
    }

}
