package qbql.lang.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;

/**
 * View table as column store database
 * Evaluate all unary predicates first
 * IndependentAttribute is such an unary relation
 * @author Dim
 *
 */
public class IndependentAttribute extends Attribute {
    Map<String, Predicate> db;
    private MaterializedPredicate content;
    public IndependentAttribute( String name, Map<String, Predicate> db ) {
        this.name = name;
        this.db = db;
    }
    
    public MaterializedPredicate getContent(){
        return content;
    }
    
    /**
     * Initialize an "unary" relation (with independent attributes and all its dependences)
     * @param root
     * @param src
     * @param varDefs
     * @param predVar
     */
    public void initContent( ParseNode root, List<LexerToken> src, Map<String, Attribute> varDefs, String predVar ) {
        ArrayList<String> attributes = new ArrayList<String>();
        attributes.add(name);
        
        for( String candidate : varDefs.keySet() ) {
            Attribute attr = varDefs.get(candidate);
            if( !attributes.contains(candidate) && attr.isDependent(name, varDefs) ) {
                attributes.add(candidate);
            }
        }
                
        content = new MaterializedPredicate(attributes, src, null);
        
        OUTER: for( ParseNode node : root.descendants() ) {
            Map<String,ParseNode> t = new HashMap<String,ParseNode>();
            t.put(name, node);
            for( String attr : attributes )
                if( !MaterializedPredicate.assignDependencyChain(attr,t,varDefs,root) )
                    continue OUTER;
            
            if( !evalUnaryPredicates(varDefs,predVar,t,root,src) )
                continue;            
            
            content.add(t);
        }
    }

    int getLimits() {
        return content.cardinality();
    }
    
    // optimization 1
    private Predicate failfastFilter = null;
    void putFilter( Predicate filter ) {
        failfastFilter = filter;        
    }
    
    private boolean evalUnaryPredicates( Map<String, Attribute> varDefs, String predVar, Map<String,ParseNode> t, ParseNode root, List<LexerToken> src ) {
        if( failfastFilter == null ) {
            Predicate fullPredicate = db.get(predVar);
            failfastFilter = unaryConjunctedPred(varDefs, fullPredicate);
        }
        return failfastFilter.eval(t, src);

    }   
    private Predicate unaryConjunctedPred( Map<String, Attribute> varDefs, Predicate fullPredicate ) {
        List<Predicate> tmp = new LinkedList<Predicate>();
        for( String s : varDefs.keySet() ) {
            Attribute attr = varDefs.get(s);
            if( attr.isDependent(name, varDefs) )
                findUnaryConjuncts(tmp, fullPredicate, attr.name);
        }
        
        Predicate ret = new True();
        for( Predicate extra : tmp )
            ret = appendProposition(extra, ret);
        return ret;
    }
    private Predicate appendProposition( Predicate extra, Predicate p ) {
        return new CompositeExpr(extra, p, Oper.CONJUNCTION);
    }
    //private void addConjunct( Predicate p, List<Predicate> ret, String nodeVar ) {
    private void findUnaryConjuncts( List<Predicate> ret, Predicate p, String nodeVar ) {
        if( p instanceof CompositeExpr ) {
            CompositeExpr ce = (CompositeExpr)p;
            if( ce.oper == Oper.CONJUNCTION ) {
                findUnaryConjuncts(ret, ce.lft,nodeVar);
                //if( !(ce.lft instanceof CompositeExpr) && lftSet.size() == 1 )
                    //ce.lft = new True();
                findUnaryConjuncts(ret, ce.rgt,nodeVar);
                //if( !(ce.rgt instanceof CompositeExpr) && rgtSet.size() == 1 )
                    //ce.rgt = new True();
            } if( ce.oper == Oper.DISJUNCTION ) {
                addDisjunct(ce, ret, nodeVar);
            }
        } else if( p instanceof NodeContent ) {
            NodeContent nc = (NodeContent)p;
            if( nc.nodeVar.equals(nodeVar) )
                ret.add(nc);
        } else if( p instanceof PositionalRelation ) {
            PositionalRelation pr = (PositionalRelation)p;
            if( pr.tA == PosType.BINDVAR && pr.b.equals(nodeVar)
             || pr.tB == PosType.BINDVAR && pr.a.equals(nodeVar) )
                ret.add(pr);
        } else if( p instanceof NodeMatchingSrc ) {
            NodeMatchingSrc nms = (NodeMatchingSrc)p;
            if( nms.nodeVar.equals(nodeVar) )
                ret.add(nms);
        }
    }
    private void addDisjunct( CompositeExpr ce, List<Predicate> ret, String nodeVar ) {
        Predicate lft = null;
        Predicate rgt = null;
        if( ce.lft instanceof NodeContent ) {
            NodeContent nc = (NodeContent)ce.lft;
            if( nc.nodeVar.equals(nodeVar) )
                lft = nc;
        }
        if( ce.lft instanceof PositionalRelation ) {
            PositionalRelation pr = (PositionalRelation)ce.lft;
            if( pr.tA == PosType.BINDVAR && pr.b.equals(nodeVar)
             || pr.tB == PosType.BINDVAR && pr.a.equals(nodeVar) )
                lft = pr;
        }
        if( ce.lft instanceof NodeMatchingSrc ) {
            NodeMatchingSrc nms = (NodeMatchingSrc)ce.lft;
            if( nms.nodeVar.equals(nodeVar) )
                lft = nms;
        }
        
        if( ce.rgt instanceof NodeContent ) {
            NodeContent nc = (NodeContent)ce.rgt;
            if( nc.nodeVar.equals(nodeVar) )
                rgt = nc;
        }
        if( ce.rgt instanceof PositionalRelation ) {
            PositionalRelation pr = (PositionalRelation)ce.rgt;
            if( pr.tA == PosType.BINDVAR && pr.b.equals(nodeVar)
             || pr.tB == PosType.BINDVAR && pr.a.equals(nodeVar) )
                rgt = pr;   // was copy and paste typo 
        }
        if( ce.rgt instanceof NodeMatchingSrc ) {
            NodeMatchingSrc nms = (NodeMatchingSrc)ce.rgt;
            if( nms.nodeVar.equals(nodeVar) )
                rgt = nms;
        }
        
        if( lft != null && rgt != null ) {
            ret.add(new CompositeExpr(ce.lft,ce.rgt,Oper.DISJUNCTION));
            //ce.lft = new True();
            //ce.rgt = new True();
        }
    }

    @Override
    ParseNode navigate(Map<String, ParseNode> nodeAssignments, ParseNode root) {
        return null;
    }

    @Override
    Attribute referredTo(Map<String, Attribute> varDefs) {
        return null;
    }   
}


