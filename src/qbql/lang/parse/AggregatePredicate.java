package qbql.lang.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;

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

    public AggregatePredicate( String attribute, Predicate predicate, boolean slash1, boolean slash2 ) {
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

    @Override
    public MaterializedPredicate eval( Parsed target ) {
        MaterializedPredicate table = predicate.eval(target);
        Integer col = table.getAttribute(attribute);
        if( col == null )
            throw new AssertionError("Predicate "+table.name+" doesn't have "+attribute+" attribute");
        
        MaterializedPredicate ret = new MaterializedPredicate(table.attributes,target.getSrc(),table.name);
        for( ParseNode[] tuple : table.tuples ) 
            eval(tuple, ret.tuples, col); 
        return ret;
    }

    /**
     * Add tuple to output if it has earlier DOB in col and matches other columns
     * @param tuple
     * @param output
     * @param col
     */
    private void eval( ParseNode[] tuple, ArrayList<ParseNode[]> output, int col ) {
        ParseNode[] match = null;
        boolean isBetter = false;
        for( ParseNode[] cmp : output ) {
            boolean matches = true;
            isBetter = false;
            for ( int i = 0; i < tuple.length; i++ ) {
                if( i == col ) {
                    boolean isEarlierDOD = tuple[col].to < cmp[col].to;
                    boolean isSameDOD = tuple[col].to == cmp[col].to;
                    boolean isLaterDOD = cmp[col].to < tuple[col].to ;
                    boolean isEarlierDOB = tuple[col].from < cmp[col].from;
                    boolean isSameDOB = tuple[col].from == cmp[col].from;
                    boolean isLaterDOB = cmp[col].from < tuple[col].from ;
                    switch( type ) {
                        case ANCESTOR:
                            if( isLaterDOD || isSameDOD && isEarlierDOB )
                                if( isEarlierDOB || isSameDOB && isLaterDOD )
                                    isBetter = true;
                            break;
                        case DESCENDANT:
                            if( isEarlierDOD || isSameDOD && isLaterDOB )
                                if( isLaterDOB || isSameDOB && isEarlierDOD )
                                    isBetter = true;
                           break;
                        case OLD:
                            if( (isEarlierDOD || isSameDOD) && (isEarlierDOB || isSameDOB) )
                                isBetter = true;
                            break;
                        case YOUNG:
                            if( (isLaterDOD || isSameDOD) && (isLaterDOB || isSameDOB) )
                                isBetter = true;
                            break;

                    }
                    continue;
                }
                if( cmp[i].from != tuple[i].from || cmp[i].to != tuple[i].to ) {
                    matches = false;
                    break;
                }               
            }
            if( !matches )
                continue;
            match = cmp;
            break;
        }
        if( match != null && isBetter )
            output.remove(match);
        if( match == null || isBetter )
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
    public boolean eval( Map<String, ParseNode> nodeAssignments, List<LexerToken> src ) {
        throw new AssertionError("N/A");
    }

    @Override
    public void variables( Set<String> ret, boolean optimizeEqs ) {
        predicate.variables(ret, false);
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

}
