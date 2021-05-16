package qbql.lang.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transitively and reflectively closed dependency map
 * If predicate is dependent and reference is empty then skip evaluation of all dependents
 * 
 * Example: symantic_analysis.prg
 * 
 * Suppose "all aggregates" is empty. Then
 * 
 * "all aggregates"                      ----> "aggregate expr" 
 * "columns&select_list&top_select_list" ----> "aggregate expr"
 * 
 * Therefore, "aggregate expr" would be empty regardless of "columns&select_list&top_select_list" 
 * and "columns&select_list&top_select_list" evaluation can be simplified to endow it with empty content 
 * 
 * However, if there is dependency which is not positive, then empty set heuristics doesn't work
 * 
 * 
 * @author Dim
 *
 */
public class PredicateDependency {
    Map<String, Map<String, Boolean>> forward = new HashMap<String, Map<String, Boolean>>();
    Map<String, Map<String, Boolean>> backward = new HashMap<String, Map<String, Boolean>>(); // redundant (to speed up transitive closure maintenance)
    
    boolean isDependent( String ref, String def ) {
        Map<String, Boolean> dependents = forward.get(ref);
        if( dependents == null )
            return false;
        return dependents.containsKey(def);
    }
    boolean isPositivelyDependent( String ref, String def ) {
        Map<String, Boolean> dependents = forward.get(ref);
        if( dependents == null )
            return false;
        return Boolean.TRUE == dependents.get(def);
    }
    
    void addDependency( String ref, String def, boolean isPositive ) {
        Map<String, Boolean> bd = backward.get(def);
        if( bd == null ) {
            bd = new HashMap<String, Boolean>();
            backward.put(def, bd);
        }
        bd.put(def,true);
        Map<String, Boolean> fr = forward.get(ref);
        if( fr == null ) {
            fr = new HashMap<String, Boolean>();
            forward.put(ref, fr);
        }
        fr.put(ref,true);        
        Map<String, Boolean> br = backward.get(ref);
        if( br == null ) {
            br = new HashMap<String, Boolean>();
            backward.put(ref, br);
        }
        br.put(ref,true);
        Map<String, Boolean> fd = forward.get(def);
        if( fd == null ) {
            fd = new HashMap<String, Boolean>();
            forward.put(def, fd);
        }
        fd.put(def,true);        
        
        Map<String, Map<String, Boolean>> newForward = new HashMap<String, Map<String, Boolean>>();
        Map<String, Map<String, Boolean>> newBackward = new HashMap<String, Map<String, Boolean>>();
        for( String from : br.keySet() )
            for( String to : fd.keySet() ) {
                boolean p1 = br.get(from);
                boolean p2 = fd.get(to);
                Map<String, Boolean> dependents = newForward.get(from);
                if( dependents == null ) {
                    dependents = new HashMap<String, Boolean>();
                    newForward.put(from, dependents);
                }
                dependents.put(to,p1&&isPositive&&p2);
                Map<String, Boolean> referents = newBackward.get(to);
                if( referents == null ) {
                    referents = new HashMap<String, Boolean>();
                    newBackward.put(to, referents);
                }
                referents.put(from,p1&&isPositive&&p2);
            }
        for( String key : newForward.keySet() ) {
            Map<String, Boolean> existing = forward.get(key);
            if( existing == null )
                forward.put(key, newForward.get(key));
            else
                existing.putAll(newForward.get(key));                    
        }
        for( String key : newBackward.keySet() ) {
            Map<String, Boolean> existing = backward.get(key);
            if( existing == null )
                backward.put(key, newBackward.get(key));
            else
                existing.putAll(newBackward.get(key));                    
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for( String from : forward.keySet() ) 
            for( String to : forward.get(from).keySet() )
                sb.append(from+(forward.get(from).get(to)?"+":"-")+"->"+to+'\n');
        return sb.toString();    
    }
    
    public static void main( String[] args ) {
        PredicateDependency pd = new PredicateDependency();
        pd.addDependency("1", "2", true);
        pd.addDependency("2", "3", true);
        pd.addDependency("0", "1", true);
        pd.addDependency("2", "4", false);
        System.out.println(pd);
    }

}
