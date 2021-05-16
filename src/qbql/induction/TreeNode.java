package qbql.induction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbql.deduction.Eq;
import qbql.symbolic.Expr;

public class TreeNode implements Expr {
    private TreeNode lft;
    private TreeNode rgt;
    String label;
    @Override
    public TreeNode left() {
        return lft;
    }
    @Override
    public TreeNode right() {
        return rgt;
    }
    @Override
    public String operation() {
        return label;
    }

    public TreeNode( TreeNode lft, TreeNode rgt ) {
        this.lft = lft;
        this.rgt = rgt;
        if( lft != null )
            complexity += lft.complexity();
        if( rgt != null )
            complexity += rgt.complexity();
    }   
    public TreeNode( TreeNode lft, String label, TreeNode rgt ) {
        this.lft = lft;
        this.rgt = rgt;
        this.label = label;
        if( lft != null )
            complexity += lft.complexity();
        if( rgt != null )
            complexity += rgt.complexity();
    }
    private int complexity = 1;
    
    
	void print() {
        System.out.println(toString()); 
    }
    String toString( int depth ) {      
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < depth ;i++)
            sb.append("  "); 
        sb.append("*\n");
        if( lft != null )
            sb.append(lft.toString(depth+1));
        if( rgt != null )
            sb.append(rgt.toString(depth+1));
        return sb.toString();
    }
    public String toString() {
        if( label == null ) {
            if( this == zero )
                return "<0>";
            if( this == one )
                return "<1>";
            if( this == two )
                return "<2>";
        }
        
        StringBuilder s = new StringBuilder();
        if( lft != null && rgt == null ) { // unary
            if( label.startsWith("<") ) {
                s.append(label);
                s.append("(");
                s.append(lft.toString());
                s.append(")");
            } else {
                s.append(lft.toString());
                s.append(label);
            }
            return s.toString();
        }
        if( lft != null ) {
            s.append("(");
            s.append(lft.toString());
            s.append(" ");
        }
        s.append(label);
        if( rgt != null ) { 
            s.append(" ");
            s.append(rgt.toString());
        }
        if( lft != null )
            s.append(")");
        return s.toString();
    }
    
    final private static TreeNode zero = new TreeNode(null,null);
    final public static TreeNode one = new TreeNode(zero,null);
    final public static TreeNode two = new TreeNode(zero,zero);
    TreeNode incr() {
        if( this == two ) return null;
        else if( this == one ) return two;
        else return one;
    }
    TreeNode zero() {
        return zero;
    }

    public TreeNode cloneLeaves() {
        if( lft == null && rgt == null )
            return new TreeNode(null,null);
        if( lft != null )
            lft = lft.cloneLeaves();
        if( rgt != null )
            rgt = rgt.cloneLeaves();
        return this;
    }
    
    long weight( boolean isExp ) {
        long ret = 1;
        if( label != null )
            ret = label.hashCode();
        if( lft != null )
            ret += lft.weight() * (isExp?2:1);
        if( rgt != null )
            ret += rgt.weight() * (isExp?2:1);
        return ret;
    }
    long weight() {
        return weight(false);
    }
    
    // algebraic optimizations
    boolean isRightSkewed() {
        boolean ret = false;
                
        if( lft != null )
            ret = lft.isRightSkewed();
        if( ret )
            return ret;
        
        if( rgt != null )
            ret = rgt.isRightSkewed();
        if( ret )
            return ret;
        
        if( "<".equals(label) || ">".equals(label) || "/<".equals(label) || "/>".equals(label) ) 
        	return false;
        
        if( lft != null && rgt != null ) {
            if( label != null && 
                ("^".equals(label) || "v".equals(label) 
               || "<AND>".equals(label) || "<OR>".equals(label) 
               || "<and>".equals(label) || "<\"and\">".equals(label) 
               || "/=".equals(label) || "/^".equals(label) 
               || "/0".equals(label) || "/1".equals(label) || "/!".equals(label) 
               || "(x)".equals(label)
           ) ) 
                return lft.weight(true) < rgt.weight(true);
        }
        return ret;
    }
    boolean isAbsorpIdemp() {
        boolean ret = false;
        if( lft != null )
            ret = lft.isAbsorpIdemp();
        if( ret )
            return ret;
        
        if( rgt != null )
            ret = rgt.isAbsorpIdemp();
        if( ret )
            return ret;
        
        if( lft != null && rgt != null ) {
            if( lft.lft != null && lft.rgt != null ) {
                long rwgt = rgt.weight(true);
                if( lft.lft.weight(true) == rwgt || lft.rgt.weight(true) == rwgt ) {
                    if( ("^".equals(label)||"v".equals(label)) 
                     && ("^".equals(lft.label)||"v".equals(lft.label)) 
                    ) return true;
                    if( ("<AND>".equals(label)||"<OR>".equals(label)) 
                     && ("<AND>".equals(lft.label)||"<OR>".equals(lft.label)) 
                           ) return true;
                }
            }
            if( rgt.lft != null && rgt.rgt != null ) {
                long lwgt = lft.weight(true);
                if( rgt.lft.weight(true) == lwgt || rgt.rgt.weight(true) == lwgt ) {
                    if( ("^".equals(label)||"v".equals(label)) 
                     && ("^".equals(rgt.label)||"v".equals(rgt.label)) 
                    ) return true;
                    if( ("<AND>".equals(label)||"<OR>".equals(label)) 
                     && ("<AND>".equals(rgt.label)||"<OR>".equals(rgt.label)) 
                           ) return true;
                }
            }
            if( lft.weight(true) == rgt.weight(true) ) 
            	if( "^".equals(label)||"v".equals(label)||"<AND>".equals(label)||"<OR>".equals(label) ) 
            	    return true;
            
        }
        
        return ret;
    }
    boolean isDoubleComplement() {
        boolean ret = false;
        if( lft != null )
            ret = lft.isDoubleComplement();
        if( ret )
            return ret;
        
        if( rgt != null )
            ret = rgt.isDoubleComplement();
        if( ret )
            return ret;
        
        if( "<NOT>".equals(label) && "<NOT>".equals(lft.label) )
            return true;
        if( ("<NOT>".equals(label)||"<INV>".equals(label)) 
         && ("R11".equals(lft.label)||"R00".equals(lft.label)) )
            return true;
        
        return ret;
    }
    
    @Override
    public Set<String> variables() {
    	Set<String> ret = new HashSet<String>();
    	if( lft == null && rgt == null) {
    		ret.add(label);
    		return ret;
    	}
        if( lft != null )
    		ret.addAll(lft.variables());
    	if( rgt != null )
    		ret.addAll(rgt.variables());
    	return ret;
    }


    public static void main( String[] args ) {
    	TreeNode l = new TreeNode(
				new TreeNode(null,"y",null),
				"^",
				new TreeNode(null,"x",null));
		TreeNode r = new TreeNode(
				new TreeNode(new TreeNode(null,"x",null),"<NOT>",null),
				"^",
				new TreeNode(null,"y",null));
		TreeNode n = new TreeNode(
    			l,
    			"<",
    			r
        );
    	System.out.println("isRightSkewed="+n.isRightSkewed());
	}
 
    @Override
    public TreeNode substitute( String x, Expr expr ) {
        TreeNode treeNode = (TreeNode)expr;
        TreeNode l = null;
        TreeNode r = null;
        TreeNode ret = null;
        if( rgt != null ) 
            r = rgt.substitute(x, treeNode);
        if( lft != null ) {
            l = lft.substitute(x, treeNode);
            ret = new TreeNode(l,label,r);
        } else if( x.equals(label) )
            ret = new TreeNode(treeNode,null);
        else
            ret = this;
        if( ret.label == null && ret.rgt == null )
            return ret.lft;
        return ret;
    }
    
    
    @Override
    public boolean equals( Object obj ) {
        if( this == obj )
            return true;
        TreeNode cmp = (TreeNode) obj;
        if( !label.equals(cmp.label) )
            return false;
        if( lft == null && rgt == null && cmp.lft == null && cmp.rgt == null )
            return true;
        if( lft == null && cmp.lft != null || lft != null && cmp.lft == null )
            return false;
        if( rgt == null && cmp.rgt != null || rgt != null && cmp.rgt == null )
            return false;
        if( lft != null && !lft.equals(cmp.lft) )
            return false;
        if( rgt != null && !rgt.equals(cmp.rgt) )
            return false;
        return true;
    }
    
    private TreeNode substitute( TreeNode e1, TreeNode e2 ) {
        if( equals(e1) )
            return e2;
        if( lft == null )
            return null;
        TreeNode res = lft.substitute(e1, e2);
        if( lft != res && res != null )
            return new TreeNode(res,label,rgt);
        if( rgt != null ) {
            res = rgt.substitute(e1, e2);
            if( rgt != res && res != null )
                return new TreeNode(lft,label,res);            
        }
        return this;
    }
    
    @Override
    public List<Expr> substitute( List<Expr> src, boolean grow ) {
        List<Expr> ret = new LinkedList<Expr>();
        for( Expr e1 : src ) for( Expr e2 : src ) {
            if( e1 == e2 )
                continue;
            if( !grow && e1.complexity() < e2.complexity() )
                continue;
            Expr tmp = substitute((TreeNode)e1,(TreeNode)e2);
            if( tmp != null && tmp != this )
                ret.add(tmp);
        }
        return ret;
    }
    
    @Override
    public int complexity() {
        return complexity;
    }
    

}
