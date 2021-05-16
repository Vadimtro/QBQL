package qbql.parser;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import qbql.lattice.Relation;
import qbql.util.Util;

/**
 * class RuleTuple, example:
 * 
 * expr : expr '+' expr
 * expr : expr '*' expr
 * 
 * head rhs
 * ---- -----------------------
 * expr {"expr", "'+'", "expr"}   
 * expr {"expr", "'*'", "expr"}   
 */
public class RuleTuple implements Comparable, Serializable {
    String head;
    String[] rhs;
    public RuleTuple( String h, List<String> r ) {
        head = h;
        rhs = new String[r.size()];
        int i = 0;
        for(String t: r)
            rhs[i++]=t;
    }
    public RuleTuple( String h, String[] r ) {
        head = h;
        rhs = r;
    }
    public boolean equals(Object obj) {
        return compareTo(obj)==0;
    }
    public int hashCode() {
        throw new RuntimeException("hashCode inconsistent with equals"); 
    }		
    public int compareTo(Object obj) {
        RuleTuple src = (RuleTuple)obj;
        int cmp = (head == null) ? 0 : head.compareTo(src.head);
        if( cmp!=0 )
            return cmp;
        if( rhs.length != src.rhs.length )
            return rhs.length - src.rhs.length;
        for(int i=0; i<rhs.length; i++)
            if( rhs[i].compareTo(src.rhs[i]) != 0 )
                return rhs[i].compareTo(src.rhs[i]);
        return 0;
    }
    /**
     * Is keyword is inside brackets? Then ignore the rule.
     */
    public boolean ignore( String keyword, String bra, String ket ) {
        boolean enteredBrackets = false; 
        for(String token: rhs) {
            if( token.equals(bra) ) {
                enteredBrackets = true;
            }
            if( enteredBrackets ) {
                if(token.equals(keyword))
                    return true;
            }
            if( token.equals(ket) ) {
                return false;
            }
        }
        return false;
    }
    public String toString() {
        StringBuffer b = new StringBuffer();
        if( head!=null )
            b.append(head+":");
        for(String t: rhs) 
            b.append(" "+t);
        return b.toString();
    }

    public String toHTML() {
        return toHTML(null);
    }
    public String toHTML( RuleTuple predecessor ) {
        StringBuffer b = new StringBuffer();
        if( predecessor == null || !predecessor.head.equals(head) )
            b.append(head+":"+Util.identln(12-head.length()-2," "));
        else
            b.append(Util.identln(10, "| "));
        for( String t: rhs )
            if( t.startsWith("'") )
                b.append(" \""+t.substring(1,t.length()-1)+"\"");
            else
                b.append(" "+t);
        return b.toString();
    }
    public static void memorizeRules( Set<RuleTuple> rules, String location ) throws Exception {
        FileOutputStream fos = new FileOutputStream(location);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(rules);
        out.close();
    }
    public static Set<RuleTuple> getRules( String location ) throws Exception {
        URL u = Relation.class.getResource( location );
        InputStream is = u.openStream();
        ObjectInputStream in = new ObjectInputStream(is);
        Set<RuleTuple> rules = (Set<RuleTuple>) in.readObject();
        in.close();
        return rules;
    }
    public static void printRules( Set<RuleTuple> rules ) {
        RuleTuple predecessor = null;
        for( RuleTuple rule : rules ) {
            System.out.println(rule.toHTML(predecessor)); 
            predecessor = rule;
        }
    }

}
