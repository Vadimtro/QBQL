package qbql.arbori;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.arbori.AncestorDescendantNodes.Type;
import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.Parsed;
import qbql.util.Util;

/**
 * Arbori program consists of symbolic predicates, which are evaluated (materialized) into multivariate relations 
 * (AKA tables in SQL world). MaterializedPredicates can be joined, projected, filtered and so on.
 * 
 * @author Dim
 */
public class MaterializedPredicate 
    extends IndependentAttribute // N-ary relation can be viewed as unary relation with one composite attribute
    implements Predicate  {
    
	Map<String,Integer> attributePositions = new HashMap<String,Integer>();
	ArrayList<String> attributes;
	Set<Tuple> tuples = new TreeSet<Tuple>();
	List<LexerToken> src;
	
	public MaterializedPredicate( ArrayList<String> attributes, List<LexerToken> src, String name ) {
	    super(name);
		this.attributes = attributes;
		for( int i = 0; i < attributes.size(); i++ ) 
			attributePositions.put(attributes.get(i), i);
		this.src = src;
		
		//this.name = name;
	}
	public MaterializedPredicate( String name, MaterializedPredicate src ) {
        super(name);
		this.attributes = src.attributes;
		this.attributePositions = src.attributePositions;
		this.tuples = src.tuples; //(ArrayList<ParseNode[]>) src.tuples.clone();
		this.src = src.src;
	}
	
	@Override
    public MaterializedPredicate getContent() {
        //return this;
        return super.getContent();
    }
	
	public Set<Tuple> getTuples() {
		return tuples;
	}
    
	/**
	 * Tuple evaluation: not well defined
	 */
    @Override
    public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
        throw new AssertionError("N/A");
    }
    /**
     * Variables for [symbolic] predicate evaluation are not needed if predicate is materialized already
     */
    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
        ret.addAll(attributes);
    }
    @Override
    public void signature(Set<String> ret) {
        variables(ret,false);        
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
		tuples.add(new Tuple(tuple));
	}
	
	void addContent( ParseNode[] tuple ) {
		tuples.add(new Tuple(tuple));
	}
	
	/**
	 * Get value of a cell in the content table
	 * @param tupleNum
	 * @param attribute
	 * @return
	 */
	public ParseNode getAttribute( Tuple tuple, String attribute ) {
		Integer attrPos = getAttribute(attribute);
		if( attrPos == null )
		    throw new AssertionError("Missing "+name+"."+attribute);
        return tuple.values[attrPos];
	}
	
	public String getAttribute( int colPos ) {
	    return attributes.get(colPos);
	}
    public Integer getAttribute( String colName ) {
    	Integer ret = attributePositions.get(colName);
    	if( ret != null )
    		return ret;
    	if( name!= null && colName.startsWith(name) )
    		ret = attributePositions.get(colName.substring(name.length()+1));
    	return ret;
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
	public void initContent( ParseNode root, List<LexerToken> src, AttributeDefinitions varDefs, String predVar ) {
	    //super.initContent(root, src, varDefs, predVar ); 
		setContent(this);
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
        if( name != null )
            ret = new StringBuilder(name+"=");
        //ret.append(header.keySet()+"\n");
        // no commas
        ret.append("[");
        for( int i = 0; i < attributes.size(); i++ )
        	ret.append((i>0?"         ":"")+attributes.get(i));
        ret.append("]\n");
        for( Tuple t : tuples ) { 
        	boolean firstTuple = true;
            for( int i = 0; i < attributes.size(); i++ ) {
        		String value = "N/A (null)";
        		ParseNode node = t.values[i];
        		if( node != null )
        		    value = "["+t.values[i].from+","+t.values[i].to+")";
        		ret.append((firstTuple?Util.identln(ident," "):"  "));
        		ret.append(value);
        		ret.append(" ");
                if( node != null )
                    ret.append(LexerToken.mnemonics(node.from, node.to, src));
        		firstTuple = false;			
            }
        	ret.append("\n");
        }
        return ret.toString();
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
        for( Tuple tupleX: x.tuples ){
            ParseNode[] retTuple = new ParseNode[header.size()];
            for( String attr : ret.attributes ) {
                retTuple[ret.attributePositions.get(attr)] = tupleX.values[x.attributePositions.get(attr)];
            }
            ret.addContent(retTuple);
        }
        for( Tuple tupleY: y.tuples ){
            ParseNode[] retTuple = new ParseNode[header.size()];
            for( String attr : ret.attributes ) {
                retTuple[ret.attributePositions.get(attr)] = tupleY.values[y.attributePositions.get(attr)];
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
        for( Tuple tupleX: x.tuples )
            for( Tuple tupleY: y.tuples ) {                
                ParseNode[] retTuple = new ParseNode[header.size()];
                for( String attr : ret.attributes ) {
                    Integer iX = x.attributePositions.get(attr);
                    Integer xAttr = iX;
                    Integer iY = y.attributePositions.get(attr);
                    Integer yAttr = iY;
                    Integer iRet = ret.attributePositions.get(attr);
                    if( xAttr == null )
                        retTuple[iRet] = tupleY.values[iY];
                    else if( yAttr == null )
                        retTuple[iRet] = tupleX.values[iX];
                    else {
                        if( tupleY.values[iY].from != tupleX.values[iX].from ||
                            tupleY.values[iY].to != tupleX.values[iX].to 
                        ) {
                            retTuple = null;
                            break;
                        } else
                            retTuple[iRet] = tupleX.values[iX];
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
        for(Tuple tupleX: x.tuples ) {
            boolean foundMatch = false;
            for( Tuple tupleY: y.tuples ) {
                boolean tuplesMatch = true;
                for( String attr : y.attributes ) {                    
                    Integer iY = y.attributePositions.get(attr);
                    Integer iX = x.attributePositions.get(attr);
                    if( iX == null ) 
                        continue;
                    
                    if( tupleY.values[iY].from != tupleX.values[iX].from ||
                            tupleY.values[iY].to != tupleX.values[iX].to ) {
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
                ret.tuples.add(tupleX);
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

    /**
     * Applies join filter 
     * @param x
     * @param y
     * @param filter
     * @param varDefs
     * @param root
     * @return
     */
    public static MaterializedPredicate filteredCartesianProduct( MaterializedPredicate x, MaterializedPredicate y, Predicate filter, Map<String,Attribute> varDefs, ParseNode root) {
        ArrayList<String> allVariables = new ArrayList<String>();
        //allVariables.addAll(x.attributes);
        //allVariables.addAll(y.attributes);
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
                    allVariables.add(z);
                    break;
                }
            }
        }
        for( String z : varDefs.keySet() ) {
            if( allVariables.contains(z) )
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
                    allVariables.add(z);
                    break;
                }
            }
        }
        
    	if( filter instanceof AncestorDescendantNodes ) {
    		AncestorDescendantNodes loop = (AncestorDescendantNodes) filter;
    		if( null == y.getAttribute(loop.d) ) {
    			MaterializedPredicate tmp = x;
    			x = y;
    			y = tmp;
    		}
    	}
       
        MaterializedPredicate ret = new MaterializedPredicate(allVariables,x.src,null);
        for( Tuple tupleY: y.tuples ) {
        	Set<Tuple> innerLoop = x.tuples; 
        	if( filter instanceof AncestorDescendantNodes ) {
        		AncestorDescendantNodes loop = (AncestorDescendantNodes) filter;
        		try {
        			Integer index = y.getAttribute(loop.d);
        			if( index == null ) {
        				Attribute attr = varDefs.get(loop.d);
        				if( attr instanceof EqualExpr ) {
        					EqualExpr eExpr = (EqualExpr) attr;
        					String d = eExpr.def;
        					int pos = d.indexOf('.');
        					if( 0 < pos ) {
        						d = d.substring(pos+1);
        						index = y.getAttribute(d);
        					}
        				}
        			}
        			innerLoop = loop.innerLoop(tupleY.values[index], x);
        		} catch( AssertionError e ) {}
        	}
        	//System.out.println(innerLoop.size());
        	for( Tuple tupleX: innerLoop ) {
                ParseNode[] retTuple = new ParseNode[allVariables.size()];
                // Can have variables not defined in eithe x or y:
                // "select columns".top_select_list^^ = "gby clause".group_by_list^^
                // Neither "select columns" nor "gby clause" has grandparent attribute
                List<String> mismatchedAttributes = new LinkedList<String>();
                for( int i = 0; i < retTuple.length; i++ ) {
                    String attr = ret.getAttribute(i);
                    Integer j = x.attributePositions.get(attr);
                    if( j != null ) {
                        retTuple[i] = tupleX.values[j];
                        continue;
                    }
                    j = y.attributePositions.get(attr);
                    if( j != null ) {
                        retTuple[i] = tupleY.values[j];
                        continue;
                    }
                    int dotPos = attr.indexOf('.'); 
                    if( 0 < dotPos ) {
                        String refPred = attr.substring(0,dotPos);
                        String simpleName = attr.substring(dotPos+1);
                        if( x.name != null && x.name.equals(refPred) ) {                          
                            j = x.attributePositions.get(simpleName);
                            if( j != null ) {
                                retTuple[i] = tupleX.values[j];
                                continue;
                            }
                        } else if ( y.name != null && y.name.equals(refPred) ) {
                            j = y.attributePositions.get(simpleName);
                            if( j != null ) {
                                retTuple[i] = tupleY.values[j];
                                continue;
                            }
                        }
                    }
                    mismatchedAttributes.add(attr);
                }
                for( String attr : mismatchedAttributes) {
                	Set<Tuple> tmp = new TreeSet<Tuple>();
                    tmp.add(new Tuple(retTuple));
                    tmp = ret.assignDependencyChain(attr, tmp, varDefs, root);
                    if( 1 < tmp.size() )
                        throw new AssertionError("1 < tmp.size()");
                    for( Tuple p : tmp )
                        if( retTuple != p.values ) 
                            throw new AssertionError("retTuple != p");
                }
                if( filter.eval(ret.attributePositions,retTuple, x.src, varDefs) ) 
                    ret.addContent(retTuple);                
            }
        }
        
        // TODO: what if filter has more than one closest ancestor predicate?
        for( String v1 : allVariables )
            for( String v2 : allVariables ) {
            	if( v1.compareTo(v2) <= 0 )
            		continue;
            	Predicate p = filter.isRelated(v1, v2, varDefs);
            	if( p instanceof AncestorDescendantNodes ) {
            		AncestorDescendantNodes ad = (AncestorDescendantNodes) p;
            		if( ad.type != Type.CLOSEST )
            			continue;
                    Integer col = ret.getAttribute(ad.a);
                    if( col == null )
                        throw new AssertionError("Predicate "+ret.name+" doesn't have "+ad.a+" attribute");
                    
                    AggregatePredicate ap = new AggregatePredicate(ad.a, ret.getAttribute(ad.d), filter);  
                    MaterializedPredicate ret1 = new MaterializedPredicate(allVariables,x.src,null);
                    for( Tuple tuple : ret.tuples ) 
                        ap.eval(tuple, ret1.tuples, col); 
                    return ret1;
            	}
            }
        
        return ret;
    }
    /**
     * Applies full filter
     * @param x
     * @param filter
     * @param varDefs
     * @param root
     * @return
     */
    public static MaterializedPredicate filter( MaterializedPredicate x, Predicate filter, Map<String,Attribute> varDefs, ParseNode root ) {

        final Set<String> signature = new HashSet<String>();   
        filter.signature(signature);
       
        ArrayList<String> header = disambigute(signature);        
                
        Map<String, Integer> xAttributePositions = x.attributePositions;
        if( x.name != null ) { // qualify names
            Map<String, Integer> tmp = new HashMap<String, Integer>();
            for( String attr : xAttributePositions.keySet() ) 
                tmp.put(x.name+"."+attr, xAttributePositions.get(attr));
            xAttributePositions = tmp;
        }
        
        MaterializedPredicate ret = new MaterializedPredicate(header,x.src,null);
        for( Tuple tupleX: x.tuples ) {
            if( filter.eval(xAttributePositions,tupleX.values, x.src, varDefs) ) {
                ParseNode[] projectedTuple = new ParseNode[header.size()];
                for( int ix = 0; ix < tupleX.values.length; ix++ ) {
                    String colName = x.getAttribute(ix);
                    Integer ip = ret.attributePositions.get(colName);
                    if( ip == null ) {
                        int pos = colName.indexOf('.');
                        if( 0 < pos ) {
                            if( signature.contains(colName) ) // attribute that is not projected away                               
                                ip = ret.attributePositions.get(colName.substring(pos+1));
                        }
                    }
                    if( ip != null ) // == null for columns projected away
                        projectedTuple[ip] = tupleX.values[ix];
                }
                for( int ip = 0; ip < projectedTuple.length; ip++ ) {  // extension columns, not in the x
                	if( projectedTuple[ip] != null ) // assigned above
                		continue;
                	String ixName = ret.getAttribute(ip);
                	Attribute ax = varDefs.get(ixName);
                	if( ax == null )
                		ax = varDefs.get(x.name + "."+ixName);
                 	projectedTuple[ip] = ax.lookup(xAttributePositions, tupleX.values, varDefs);
                }
                ret.addContent(projectedTuple);
            }
        }
        return ret;
    }
    
    /**
     * Keep name qualifiers if there is more than one attribute with the same name
     * @param signature
     * @return
     */
    private static ArrayList<String> disambigute( Set<String> signature ) {
        Map<String, String> names = new HashMap<String, String>();
        for( String candidate : signature ) {
            String name = candidate;
            int pos = candidate.indexOf('.');
            if( 0 < pos )
                name = name.substring(pos+1);   
            if( names.containsKey(name) )
                names.put(name, candidate);
            else
                names.put(name, name);
        }            
        ArrayList<String> ret = new ArrayList<String>();
        for( String candidate : signature ) {
            String name = candidate;
            int pos = candidate.indexOf('.');
            if( 0 < pos )
                name = name.substring(pos+1);
            String tmp = names.get(name);
            if( 0 < tmp.indexOf('.') )
                ret.add(candidate);
            else
                ret.add(name);
        }
        return ret;
    }
    
    /**
     * Attribute.navigate amended to recursive navigation
     * @param name - attribute name
     * @param nodeAssignments
     * @param varDefs
     * @param root
     * @return mutated list 
     */
    Set<Tuple> assignDependencyChain( String name, Set<Tuple> candidates, Map<String,Attribute> varDefs, ParseNode root ) {
        if( candidates.size() == 0 )
            return candidates;
        for( Tuple t : candidates ) {
            final ParseNode cell = t.values[attributePositions.get(name)];
            if( cell != null )
                return candidates;  // already assigned
            else
                break;
        }
        
        Attribute attr = varDefs.get(name);
        if( attr instanceof MaterializedPredicate )
            return candidates;  
        
        Attribute ref = attr.referredTo(varDefs);
        if( ref == null )
            return candidates; // variable not joined yet
        
        candidates = assignDependencyChain(ref.name, candidates, varDefs, root);
         
        return attr.eval(attributePositions, candidates, root);       
    }
    
    
    @Override
    public Map<String, Boolean> dependencies() {        
        return new HashMap();
    }

    /**
     * Helper method to print abbreviated tuple content (e.g. in Arbor callback method)
     * e.g.
     * descendant.node = [68,69) employees ,  ancestor.node = [38,86) "corPB"af" 
     * while tuple.toString would output node payload (which is not interesting because node payload is constrained by the arbori program)
     * {descendant.node=[68,69)   cartesian_product  identifier  query_table_expression  query_table_expression[1252,1269)  table_reference  table_reference[1304,1338)  table_reference_or_join_clause, ancestor.node=[38,86)   create  create_plsql  sql_statement}
     * @param tuple   
     * @param src
     * @return
     */
    public static String tupleMnemonics( Map<String,ParseNode> tuple, List<LexerToken> src ) {
        return tupleMnemonics(tuple, src, false);
    }
    public static String tupleMnemonics( Map<String,ParseNode> tuple, List<LexerToken> src, boolean independentOnly ) {
        //System.out.println(tuple.toString());
        StringBuilder ret = new StringBuilder();
        int num = -1;
        for( String attribute : tuple.keySet()  ) {
            if( independentOnly && (0<=attribute.indexOf('^') || 0<=attribute.indexOf('+') || 0<=attribute.indexOf('-')) )
                continue;
            num++;
            if( 0 < num )
                ret.append(",  ");
            ret.append( attribute + " = ");
            ParseNode node = tuple.get(attribute);
            if( node != null )
                ret.append( "["+node.from+","+node.to+")" );
            ret.append(" ");
            if( node != null )
                ret.append(LexerToken.mnemonics(node.from, node.to, src));
        }
        return ret.toString();
    }
    
    @Override
    public Predicate copy( Program prg ) {
        return this;
    }
    
    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}


}
