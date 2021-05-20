package qbql.arbori;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.Parsed;

/*
 * Runtime evaluation for upper & lower bound operator
 * In SQL terms it is 
 *    select "max"(attribute), rest of attributes from predicate group by rest of attributes
 */
public class AggregatePredicate implements Predicate {
    public enum Type {/*[[))*/ ANCESTOR,DESCENDANT, /*[)[)*/ OLD,YOUNG,}; 
    private Type type;
    String attribute;
    Predicate predicate;
    private int ancestor = -1; 
    
    public AggregatePredicate( String descendant, int ancestor, Predicate predicate ) {
        this(descendant, predicate, /*/\ :*/ true, false);
        this.ancestor = ancestor;
    }
    
    public AggregatePredicate( String attribute, Predicate predicate, boolean slash1, boolean slash2 ) {
    	Set<String> signature = new HashSet<String>();
    	predicate.signature(signature);
    	if( !signature.contains(attribute) ) {
    		boolean found = false;
    		for( String attr : signature ) {
    			int pos = attr.indexOf('.');
    			if( 0 < pos ) {
    				attr = attr.substring(pos+1);
    				if( attr.equals(attribute) ) {
    					found = true;
    					break;
    				}
    					
    			}
    		}
    		if( !found )	
    			throw new AssertionError("missing "+attribute+ " in "+signature+"");
    	}
        if( !slash1 && !slash2 )
            type = Type.OLD;
        else if( !slash1 && slash2 )
            type = Type.ANCESTOR;
        else if( slash1 && !slash2 )
            type = Type.DESCENDANT;
        else if( slash1 && slash2 )
            type = Type.YOUNG;
        this.attribute = attribute;
        this.predicate = predicate;
    }

    public AggregatePredicate( AggregatePredicate source ) {
        type = source.type;
        attribute = source.attribute;
        predicate = source.predicate;
        ancestor = source.ancestor;
    }

    @Override
    public MaterializedPredicate eval( Parsed target ) {
        MaterializedPredicate table = predicate.eval(target);
        Integer col = table.getAttribute(attribute);
        if( col == null )
            throw new AssertionError("Predicate "+table.name+" doesn't have "+attribute+" attribute");
        
        MaterializedPredicate ret = new MaterializedPredicate(table.attributes,target.getSrc(),table.name);
        for( Tuple tuple : table.tuples ) 
            eval(tuple, ret.tuples, col); 
        return ret;
    }

    /**
     * Add tuple to output if it has earlier DOB in col and matches other columns
     * @param tuple
     * @param output
     * @param col
     */
    void eval( Tuple tuple, Set<Tuple> output, int col ) {
        Tuple inferior = null;
        Tuple superior = null;
        for( Tuple cmp : output ) {
            boolean sameProjection = true;
            Tuple inf = null;
            Tuple sup = null;
            for ( int i = 0; i < tuple.values.length; i++ ) {
                if( i == col ) {
                    boolean isEarlierDOD = tuple.values[col].to < cmp.values[col].to;
                    boolean isSameDOD = tuple.values[col].to == cmp.values[col].to;
                    boolean isLaterDOD = cmp.values[col].to < tuple.values[col].to ;
                    boolean isEarlierDOB = tuple.values[col].from < cmp.values[col].from;
                    boolean isSameDOB = tuple.values[col].from == cmp.values[col].from;
                    boolean isLaterDOB = cmp.values[col].from < tuple.values[col].from ;
                    switch( type ) {
                        case ANCESTOR:
                            if( isLaterDOD || isSameDOD && isEarlierDOB )
                                if( isEarlierDOB || isSameDOB && isLaterDOD )
                                    inf = cmp;
                            if( isEarlierDOD || isSameDOD && isLaterDOB )
                                if( isLaterDOB || isSameDOB && isEarlierDOD )
                                    sup = cmp;
                           break;
                        case DESCENDANT:
                            if( isEarlierDOD || isSameDOD && isLaterDOB )
                                if( isLaterDOB || isSameDOB && isEarlierDOD )
                                    inf = cmp;
                            if( isLaterDOD || isSameDOD && isEarlierDOB )
                                if( isEarlierDOB || isSameDOB && isLaterDOD )
                                    sup = cmp;
                           break;
                        case OLD:
                            if( (isEarlierDOD || isSameDOD) && (isEarlierDOB || isSameDOB) )
                                inf = cmp;
                            if( (isLaterDOD || isSameDOD) && (isLaterDOB || isSameDOB) )
                                sup = cmp;
                            break;
                        case YOUNG:
                            if( (isLaterDOD || isSameDOD) && (isLaterDOB || isSameDOB) )
                                inf = cmp;
                            if( (isEarlierDOD || isSameDOD) && (isEarlierDOB || isSameDOB) )
                                sup = cmp;
                            break;

                    }
                    continue;
                }
                if( (ancestor == -1 || ancestor == i)
                 && (cmp.values[i].from != tuple.values[i].from || cmp.values[i].to != tuple.values[i].to)
                ) {
                    sameProjection = false;
                    break;
                }
            }
            if( sameProjection ) {
                if( inferior == null )
                    inferior = inf;
                if( superior == null )
                    superior = sup;
            }
        }
        if( inferior != null ) 
            output.remove(inferior);
        if( superior == null )
            output.add(tuple);
    }

    @Override
    public String toString( int depth ) {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < depth ;i++ )
            sb.append("  ");  //$NON-NLS-1$
        sb.append(attribute);
        switch( type ) {
            case ANCESTOR:
                sb.append("\\/");
                break;
            case DESCENDANT:
                sb.append("/\\");
                break;
            case OLD:
                sb.append("\\\\");
                break;
            case YOUNG:
                sb.append("//");
                break;

        }
        sb.append(predicate.toString(depth));
        return sb.toString();
    }

    @Override
    public boolean eval( Map<String, Integer> attributePositions, ParseNode[] tuple, List<LexerToken> src, Map<String,Attribute> varDefs ) {
        throw new AssertionError("N/A");
    }

    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
        predicate.variables(ret, false);
    }
    @Override
    public void signature( Set<String> ret ) {
        predicate.signature(ret);
    }

    /*@Override
    public void eqNodes(Map<String, Attribute> varDefs) {
        throw new AssertionError("N/A");
    }*/

    @Override
    public Predicate isRelated( String var1, String var2, Map<String,Attribute> varDefs ) {
        return predicate.isRelated(var1, var2, varDefs);
    }

    @Override
    public Map<String, Boolean> dependencies() {
        return predicate.dependencies();
    }

    @Override
    public Predicate copy( Program prg ) {
        AggregatePredicate ret = new AggregatePredicate(this);
        ret.predicate = predicate.copy(prg);
        return ret;
    }

    @Override
	public void adjustIsFunFlag(Set<String> independentAttributes) {		
	}
}
