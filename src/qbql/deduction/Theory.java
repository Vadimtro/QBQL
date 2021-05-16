package qbql.deduction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import qbql.symbolic.Expr;
import qbql.util.Array;
import qbql.util.Util;

public class Theory {
    List<Eq> assertions = new LinkedList<Eq>();
    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for( Eq e1 : assertions )
            ret.append(e1.toString()+'\n');
        return ret.toString();
    }

    public void add( Theory theory ) {
        for( Eq eq : theory.assertions )
            add(eq);
    }
    
    public void add( Eq eq ) {
        if( eq.size() == 0 )
            return;
        Eq add = eq;
        int[] indexes = new int[0];
        int i = -1;
        for( Eq e : assertions ) {
            i++;
            if( e.equals(eq) ) {
                add = Eq.merge(e, add); 
                indexes = Array.insert(indexes, i);
            }
        }
        for( int j = indexes.length-1; 0 <= j; j-- ) 
            assertions.remove(indexes[j]);        
        assertions.add(add);
    }
    
    public void reduce() {
        int[] indexes = new int[0];
        int i = -1;
        for( Eq e : assertions ) {
            i++;
            for( Eq eq : assertions ) 
                if( e != eq && e.equals(eq) ) {
                    indexes = Array.insert(indexes, i);
                    break;
                }
        }
        for( int j = indexes.length-1; 0 <= j; j-- ) 
            assertions.remove(indexes[j]);        
    }

    
    Theory substitute( String variable, Expr expr ) {
        Theory ret = new Theory();
        for( Eq eq : assertions )
            ret.add(eq.substitute(variable, expr));
        return ret;
    }
    
    Theory assign( Set<String> variables ) {
        Theory ret = new Theory();
        String[] vars = variables.toArray(new String[0]);
        
        for( Eq eq : assertions ) {
            final Set<String> allVars = new HashSet<String>();
            for( Expr ge : eq.expressions )
                allVars.addAll(ge.variables());
            
            int[] indexes = new int[allVars.size()];
            for( int i = 0; i < indexes.length; i++ )
                indexes[i] = 0;
            do {
                Eq tmp = eq;
                int var = 0;
                for( String variable : allVars.toArray(new String[0]) ) { 
                    tmp = tmp.substitute(variable,Prover.compose(null,"!"+vars[indexes[var++]],null));
                } 
                for( String variable : vars )  
                    tmp = tmp.substitute("!"+variable,Prover.compose(null,variable,null));
                ret.add(tmp);
                
            } while( Util.next(indexes,variables.size()) );
            
        }
        
        
        return ret;
    }
    
    private void step( int i, int j ) {
        Eq e1 = assertions.get(i);
        Eq e2 = assertions.get(j);
        Eq o = e1.leverage(e2, false);
        if( o.size() == 0 )
            return;
        add(Eq.merge(o, e1));
    }
    
    private boolean step( int i ) {
        int assertionsSize = assertions.size();
        int qlassSize = assertions.get(i).size();
        for( int j = 0; j < assertions.size(); j++ ) {
            step(i,j);
            
            //System.out.println("----- Step (" +i+','+j+ ") ----- : ");
            //System.out.println(toString());  
            
            //if( 10 < assertions.get(i).size() )
                //System.exit(0);
            if( assertions.size() < assertionsSize ) 
                return false;
            else if( assertions.get(i).size() != qlassSize )
                break;
        }
        return true;
    }
    
    public void grow( Expr src, Expr with ) {
        Eq e1 = null;
        for( Eq eq : assertions )
            if( eq.contains(src) ) {
                e1 = eq;
                break;
            }
        Eq e2 = null;
        for( Eq eq : assertions )
            if( eq.contains(with) ) {
                e2 = eq;
                break;
            }
        Eq o = e1.leverage(e2, true);
        add(Eq.merge(o, e1));
    }


    
    void step() {
        for( int i = 0; i < assertions.size(); i++ ) 
            if( !step(i) )
                return;                    
    }

    public int complexity() {
        int ret = 0;
        for( Eq e : assertions ) 
            ret += e.complexity();
        return ret;
    }

    public int size() {
        return assertions.size();
    }
    

}
