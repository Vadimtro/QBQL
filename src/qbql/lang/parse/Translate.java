package qbql.lang.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.util.Util;

public class Translate {
    public static void main( String[] args ) throws Exception {
        
        final Parsed prog = new Parsed(
                Util.readFile(Program.class, "translate.prg"), //$NON-NLS-1$
                //Util.readFile("c:/qbql_trunm/.../semantic_analysis.prg"), //$NON-NLS-1$
                //Service.readFile(Rewrite.class, "start_block_proc_args.prg"), //$NON-NLS-1$
                Program.getArboriParser(),
                "program" //$NON-NLS-1$
        );
        if( false  )
            prog.getRoot().printTree();    

        final Parsed target = new Parsed(
                Util.readFile(Program.class, "RL.axiom"), 
                qbql.lattice.Program.earley,
                "program" //$NON-NLS-1$
        );
        target.isQuotedString = false;
        if( false  )
            target.getRoot().printTree();        

        Program r = new Program(qbql.lattice.Program.earley);
        r.program(prog.getRoot(), prog.getSrc(), prog.getInput());
        
        target.getRoot(); // to get better idea of timing
        long t1 = System.currentTimeMillis();
        //Service.profile(10000, 100, 5);
        Map<String,MaterializedPredicate> output = r.eval(target);
        //for( String p : output.keySet() )
            //System.out.println(p+"="+output.get(p).toString(p.length()+1));
        //if( debug ) 
        System.out.println("\n *********** eval time ="+(System.currentTimeMillis()-t1)+"\n");
        
        
        ParseNode root = null;
        MaterializedPredicate f = output.get("formulas"); //$NON-NLS-1$
        for( int i = 0; i < f.cardinality(); i++) {
            root = f.getAttribute(i, "implication");
            break;
        }

        Map<String,Integer> variables = new HashMap<String,Integer>();
        MaterializedPredicate col = output.get("variables"); //$NON-NLS-1$
        for( int i = 0; i < col.cardinality(); i++) {
            ParseNode node = col.getAttribute(i, "identifier");
            //int begin = target.getSrc().get(node.from).begin;
            //int end = target.getSrc().get(node.to-1).end;
            String name = target.getSrc().get(node.from).content;
            if( variables.get(name) == null )
                variables.put(name, variables.size());
        }
        System.out.println(variables);
        
        System.out.println(translate(root,target.getSrc(),variables).expr+'.');
    }

    static class Expr {
        String expr;
        List<String> free;
        Expr( String expr, List<String> free ) {
            this.expr = expr;
            this.free = free;
        }
        Expr() {
            this("$F",new ArrayList<String>());
        }
        Expr( String name, int i, int len ) {
            free = parameters(i, len);
            //bind = new ArrayList();
            String varName = name+"(";
            StringBuilder tmp = new StringBuilder(varName);
            for( String par : free ) {
                if( varName.length() < tmp.length() )
                    tmp.append(',');
                tmp.append(par);
            }
            tmp.append(")");
            expr = tmp.toString();
        }
        Expr( Expr e1, Expr e2, String oper ) {
            free = new ArrayList();
            List<String> bind = new ArrayList();
            if( "v".equals(oper) ) {
                List<String> common = new ArrayList();
                for( String p1 : e1.free ) 
                    for( String p2 : e2.free )
                        if( p1.equals(p2) && !common.contains(p1))
                            common.add(p1);
                for( String p1 : e1.free ) {
                    if( !bind.contains(p1) && !common.contains(p1) )
                        bind.add(p1);
                }
                for( String p2 : e2.free ) {
                    if( !bind.contains(p2) && !common.contains(p2) )
                        bind.add(p2);
                }                
            }
            for( String p1 : e1.free ) {
                if( !free.contains(p1) && !bind.contains(p1) )
                    free.add(p1);
            }
            for( String p2 : e2.free ) {
                if( !free.contains(p2) && !bind.contains(p2) )
                    free.add(p2);
            }
            StringBuilder tmp = new StringBuilder();
            for( String par : bind ) 
                tmp.append(" exists "+par);
            
            tmp.append("(");
            tmp.append(e1.expr);
            if( oper.equals("^") )
                tmp.append("&");
            if( oper.equals("v") )
                tmp.append(" | ");
            if( oper.equals("=") )
                tmp.append("\n<->\n");
            tmp.append(e2.expr);
            tmp.append(")");
            expr = tmp.toString();
        }
        void amend( String newExpr ) {
            expr = newExpr;
        }
    }   
    static Expr translate( ParseNode node, List<LexerToken> src, Map<String,Integer> variables ) {
        if( node.from+1 == node.to ) {
            String name = src.get(node.from).content;
            if( "R00".equals(name) )
                return new Expr();
            Integer pos = variables.get(name);
            return new Expr(name,pos,variables.size());
        }
        ParseNode arg1 = null;
        ParseNode oper = null;
        ParseNode arg2 = null;
        for( ParseNode child : node.children() ) {
            if( arg1 == null ) {
                arg1 = child;
                continue;
            } else if( oper == null ) {
                if( node.contains(qbql.lattice.Program.parExpr) )
                    return translate(child,src,variables);
                if( node.contains(qbql.lattice.Program.complement) ) {
                    Expr ret = translate(arg1,src,variables);
                    ret.amend("-("+ret.expr+")");
                    return ret;
                };
                if( node.contains(qbql.lattice.Program.inverse) ) {
                    Expr target = translate(arg1,src,variables);
                    List<String> others = new ArrayList<String>();
                    long pow2 = 1 << variables.size();
                    for( long j = 1; j < pow2; j++) 
                        others.add("p"+j);
                    StringBuilder indicator = new StringBuilder("(");
                    for( String v : target.free ) {
                        indicator.append(" exists "+v);
                        if( others.contains(v) )
                            others.remove(v);
                    }
                    indicator.append(" "); 
                    indicator.append(target.expr); 
                    indicator.append(")"); 
                    for( String name : variables.keySet() ) {
                        int i = variables.get(name);
                        List<String> params = parameters(i, variables.size());
                        List<String> used = new ArrayList();
                        for( String o : others )
                            if( params.contains(o) )
                                used.add(o);
                        if( 0 == used.size() )
                            continue;
                        indicator.append("&("); 
                        for( String p : params )
                            if( !used.contains(p) )
                                indicator.append(" exists "+p);
                        indicator.append(" ");
                        indicator.append(name);
                        indicator.append("(");
                        int len = indicator.length();
                        for( String par : params ) {
                            if( len < indicator.length() )
                                indicator.append(',');
                            indicator.append(par);
                        }
                        indicator.append(")|$T)");

                    }
                    return new Expr(indicator.toString(),others);
                };
                oper = child;
                continue;
            } else if( arg2 == null ) {
                arg2 = child;
                continue;
            }
        }
        return new Expr(translate(arg1,src,variables),translate(arg2,src,variables),src.get(oper.from).content);
            
    }
    
    static List<String> parameters( int i, int len ) {
        List<String> ret = new ArrayList<String>();
        long pow2 = 1 << len;
        for( long j = 0; j < pow2; j++) {
            if( (j & (1L << i)) != 0 )
                ret.add("p"+j);
        }
        return ret;
    }

}
