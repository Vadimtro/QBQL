package qbql.arbori;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import qbql.arbori.AncestorDescendantNodes.Type;
import qbql.parser.Parsed;

/**
 * Map of all attributes found in the named predicate definition
 * @author Dim
 *
 */
public class AttributeDefinitions extends HashMap<String, Attribute> {

    final Map<String, Predicate> namedPredicates;   // context (database) of all named predicates
    
    //private final Map<String, IndependentAttribute> dimensions;  // Evaluated IndependentAttributes 
    private Set<String> independentAttributes;
    
    private String evaluatedPredVar;
    
    private ArrayList<String> header;
    
    private Predicate evaluatedPredicate;
    
    public AttributeDefinitions( String evaluatedPredVar, Map<String, Predicate> namedPredicates ) {
        this.namedPredicates = namedPredicates;
        //this.dimensions = new HashMap<String, IndependentAttribute>();
        this.independentAttributes = new HashSet();
        this.evaluatedPredVar = evaluatedPredVar;
        final Set<String> allVariables = new HashSet<String>();  // x,y,z,x^,x+1,predicate.attr, 
        // contains temporary entries e.g. x=expr
        evaluatedPredicate = namedPredicates.get(evaluatedPredVar);
        evaluatedPredicate.variables(allVariables, true);       

        Set<String> removal = new HashSet<String>();
        for( String s : allVariables ) 
            extractDependentAttributes(s, allVariables,removal);
        
        independentAttributes.removeAll(removal); 
        
        evaluatedPredicate.adjustIsFunFlag(independentAttributes);
    }

    private void extractDependentAttributes( String name, Set<String> allVariables, Set<String> removal ) {
        //if( "sc.id".equals(name) )
            //name = "sc.id";
        
        if( containsKey(name) )
            return;
                       
        String ref = Attribute.referredTo(name);
        if( ref != null ) {
            if( 0 < name.indexOf('<') ) {
                int pos = name.indexOf('<');
                String prefix = name.substring(0,pos);
                String postfix = name.substring(pos+1);
                Type type = Type.CLOSEST;
                if( postfix.charAt(0)=='=' || postfix.charAt(0)=='<' ) {
                    postfix = postfix.substring(1);
                    type = Type.TRANSITIVE;
                }
                // Cumbersome navigation in those cases:
                if( 0 < prefix.indexOf('.') 
                 || 0 < prefix.indexOf('^') 
                 || 0 < prefix.indexOf('+') 
                 || 0 < prefix.indexOf('-') ) {
                    extractDependentAttributes(prefix,allVariables,removal);
                    extractDependentAttributes(postfix,allVariables,removal);
                    return;
                }
                
                AncestorExpr ee = new AncestorExpr(prefix,postfix,type,evaluatedPredicate);
                put(ee.name, ee);
                removal.add(ee.name);   
                extractDependentAttributes(ee.name,allVariables,removal);
                return;
 
            } else if( 0 < name.indexOf('=') ) {
                int pos = name.indexOf('=');
                String prefix = name.substring(0,pos);
                String postfix = name.substring(pos+1);
                
                String prefRef = Attribute.referredTo(prefix);
				String postRef = Attribute.referredTo(postfix);
				if( null == prefRef && null == postRef ) {
					// select the most referenced Attribute
					int prefCnt = 0;
					int postCnt = 0;
					for( String s : allVariables ) {
				        StringTokenizer st = new StringTokenizer(s,"=^<>[)?");
				        while( st.hasMoreTokens() ) {
				        	String token = st.nextToken();
				        	if( prefix.equals(token) )
				        		prefCnt++;
				        	if( postfix.equals(token) )
				        		postCnt++;
				        }
					}
					if(  postCnt < prefCnt)
						prefRef = "not null";
				} 
				if( null == prefRef ) {
                    EqualExpr ee = new EqualExpr(prefix,postfix);
                    put(ee.name, ee);
                    removal.add(ee.name);   
                    ref = postfix;
				} else if( null == postRef ) {
					EqualExpr ee = new EqualExpr(postfix,prefix);
					put(ee.name, ee);
					removal.add(ee.name);   
					ref = prefix;
				} else {
					extractDependentAttributes(prefix,allVariables,removal);
					extractDependentAttributes(postfix,allVariables,removal);
					return;
				}
				
 
            } else if( name.endsWith("^") )
                put(name, new Parent(name));
            else if( name.charAt(ref.length()) == '-' ) {
				try {
					String suffix = name.substring(ref.length());
					final int n = Integer.parseInt(suffix);
					put(name, new Sibling(name,n));
				} catch( NumberFormatException e ) {
					throw new AssertionError("Cant parse number in "+name);
				}
            } else if( name.charAt(ref.length()) == '+' ) {
				try {
					String suffix = name.substring(ref.length());
					final int n = Integer.parseInt(suffix);
					put(name, new Sibling(name,n));
				} catch( NumberFormatException e ) {
					throw new AssertionError("Cant parse number in "+name);
				}
            } else if( 0 < name.indexOf('.') ) {
                String pred = name.substring(0,name.indexOf('.'));
                MaterializedPredicate refPred = (MaterializedPredicate)namedPredicates.get(pred);
                String postfix = name.substring(pred.length()+1);
                //if( !postfix.equals(ref)  )
                    //throw new AssertionError("!postfix.equals(ref)"); //$NON-NLS-1$
                if( refPred.getAttribute(postfix) == null  )
                    throw new AssertionError("Undefined variable "+postfix+" in "+pred);
                put(name, new Column(name));
                ref = pred;
            } else
                throw new AssertionError("unexpected case"); //$NON-NLS-1$
            extractDependentAttributes(ref, allVariables,removal);
        } else {
            put(name, new IndependentAttribute(name));
            independentAttributes.add(name);
            
            Predicate pred = namedPredicates.get(name);
            if( pred != null ) {
                if( !(pred instanceof MaterializedPredicate) )
                    throw new AssertionError(" !("+name+" instanceof MaterializedPredicate)"); //$NON-NLS-1$
                MaterializedPredicate defVectors = (MaterializedPredicate) pred;
                put(name,defVectors/*new MaterializedPredicate(defVectors)*/);
                defVectors.name = name;
            }            
        }            
    }

    public void evalDimensions( Parsed target, boolean emptyContent) {
        final Predicate evaluatedPredicate = namedPredicates.get(evaluatedPredVar);
        if( Program.debug )
            System.out.println(evaluatedPredicate.toString());
        final Set<String> signature = new HashSet<String>();   
        evaluatedPredicate.signature(signature);   
                
        header = new ArrayList<String>();
        for( String candidateAttr : keySet() )
            if( signature.contains(candidateAttr) )
                header.add(candidateAttr);

        //if( Program.debug )
            //System.out.println("Independent variables = "+independentVars);

        for( String var : independentAttributes ) {
            IndependentAttribute varDef = (IndependentAttribute) get(var);
            if( !emptyContent ) {
                if( independentAttributes.size() == 1 )
                    varDef.putFilter(evaluatedPredicate);
                varDef.initContent(target.getRoot(),target.getSrc(),this,evaluatedPredVar);
            } 
        }
    }

    
    public Set<String> listDimensions() {
        return independentAttributes;
    }   
    
    public MaterializedPredicate getDimensionContent( String name ) {
        Attribute ret = get(name); 
        return ((IndependentAttribute)ret).getContent();
    }

    public ArrayList<String> getHeader() {
        return header;
    }

    /**
     * Ad-hock greedy optimization
     * @param joinedCardinality 
     * @return null if no suitable attribute to join found (e.g. the predicate may have disjunction in it, etc)
     */
    String minimalRelatedDimension( Set<String> joined, int joinedCardinality ) {
        String ret = null;
        boolean isEmpty = false;
        for( String current : independentAttributes) {
            if( getDimensionContent(current).cardinality() == 0 )
                isEmpty = true;
            if( joined.contains(current) )
                continue;
            if( !isConnected(joined,current) && !isEmpty && 1 < joinedCardinality) 
                continue;
            if( ret == null || getDimensionContent(current).cardinality() < getDimensionContent(ret).cardinality()  )
                ret = current;
        }
        return ret;
    }
    private boolean isConnected( Set<String> joined, String current ) {
        Predicate p = namedPredicates.get(evaluatedPredVar);
        for( String j : joined ) {
            if( isRelated(current, j, p) )
                return true;
        }
        return false;
    }
    private boolean isRelated( String s1, String s2, Predicate p ) {
        for( String d1 : functions(s1) )
            for( String d2 : functions(s2) )
                if( p.isRelated(d1, d2, this) != null )
                    return true;            
        return false;
    }
    private List<String> functions( String s ) {
        List<String> ret = new LinkedList<String>();
        for( String vd : keySet() )
            if( vd.startsWith(s) )
                ret.add(vd);
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(super.toString());
        ret.append("\nindependentAttributes = "+independentAttributes.toString());
        ret.append("\nevaluatedPredVar = "+evaluatedPredVar);
        ret.append("\nheader = "+header);
        return ret.toString();
    }

    public static void main( String[] args ) {
        Predicate p1 = new AncestorDescendantNodes("a.b", "c", AncestorDescendantNodes.Type.TRANSITIVE);
        Set<String> ret = new HashSet<String>();
        p1.signature(ret);
        System.out.println(ret);
    }

}
